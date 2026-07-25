package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds the open server-sent event streams, keyed by project, and fans project-change events out
 * to every stream watching that project.
 *
 * <p>Delivery never happens on the caller's thread: {@link #publish} hands each send to a dispatch
 * executor, and the heartbeat scheduler enqueues its keep-alives on the same executor. Writes to a
 * single emitter are serialized on the subscriber monitor so concurrent live sends and heartbeats
 * cannot interleave and corrupt the stream. Any failed send evicts the dead emitter.
 *
 * <p>A subscriber that reconnects with a {@code lastEventId} buffers its live events from the moment
 * it subscribes until {@link #completeCatchUp} replays the events it missed and releases the buffer.
 * Buffering under the same monitor is what lets the reconnect emit its history replay ahead of any
 * newer live event, and drop the live events the replay already covered.
 */
@Component
public class SseStreamRegistry {

    /** SSE {@code event:} name; the client filters on this. Matches the STOMP payload contract. */
    static final String EVENT_NAME = "project-events";

    static final String HEARTBEAT_COMMENT = "keep-alive";

    private static final Logger LOGGER = LoggerFactory.getLogger(SseStreamRegistry.class);

    private final ConcurrentMap<ProjectId, Set<Subscriber>> subscribersByProject = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    private final SseProperties properties;

    private final ExecutorService dispatchExecutor;

    private final ScheduledExecutorService heartbeatExecutor;

    @Autowired
    public SseStreamRegistry(ObjectMapper objectMapper, SseProperties properties) {
        this(objectMapper,
             properties,
             Executors.newCachedThreadPool(daemonThreadFactory("sse-dispatch")),
             Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("sse-heartbeat")));
    }

    SseStreamRegistry(ObjectMapper objectMapper,
                      SseProperties properties,
                      ExecutorService dispatchExecutor,
                      ScheduledExecutorService heartbeatExecutor) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.dispatchExecutor = dispatchExecutor;
        this.heartbeatExecutor = heartbeatExecutor;
    }

    @PostConstruct
    void startHeartbeat() {
        long intervalMillis = properties.getHeartbeatInterval().toMillis();
        heartbeatExecutor.scheduleAtFixedRate(this::heartbeat, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        heartbeatExecutor.shutdownNow();
        dispatchExecutor.shutdownNow();
    }

    /**
     * Open a stream for a viewer of {@code projectId} and register it for live delivery.
     *
     * @param lastEventId      the last event id the client already has, or {@code null} for a fresh
     *                         connection. A non-null value starts the stream buffering live events
     *                         until {@link #completeCatchUp} flushes them behind the history replay.
     * @param executionContext the identity/authorization context resolved for this connection.
     */
    public SseEmitter subscribe(ProjectId projectId,
                                @Nullable String lastEventId,
                                ExecutionContext executionContext) {
        return register(projectId,
                        new SseEmitter(properties.getStreamTimeout().toMillis()),
                        lastEventId,
                        executionContext);
    }

    SseEmitter register(ProjectId projectId,
                        SseEmitter emitter,
                        @Nullable String lastEventId,
                        ExecutionContext executionContext) {
        Subscriber subscriber = new Subscriber(emitter, lastEventId, executionContext);
        subscribersByProject.compute(projectId, (key, existing) -> {
            Set<Subscriber> set = (existing != null) ? existing : ConcurrentHashMap.newKeySet();
            set.add(subscriber);
            return set;
        });
        emitter.onCompletion(() -> remove(projectId, subscriber));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(projectId, subscriber);
        });
        emitter.onError(error -> remove(projectId, subscriber));
        return emitter;
    }

    /**
     * Fan {@code payload} out to every stream watching {@code projectId}, stamped with
     * {@code sequenceId} as the SSE {@code id:}. Returns immediately; sends run on the dispatch
     * executor. A stream still catching up buffers the event instead of sending it.
     */
    public void publish(ProjectId projectId, long sequenceId, Object payload) {
        Set<Subscriber> subscribers = subscribersByProject.get(projectId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        String json = serialize(projectId, payload);
        if (json == null) {
            return;
        }
        for (Subscriber subscriber : subscribers) {
            dispatchExecutor.execute(() -> deliverLive(projectId, subscriber, sequenceId, json));
        }
    }

    /**
     * Finish a reconnecting subscriber's catch-up. When {@code replayPayload} is non-null it is sent
     * as a single catch-up frame stamped with {@code caughtUpThroughSeq}; then the live events
     * buffered while the history query ran are released in order, dropping any with seq
     * {@code <= caughtUpThroughSeq} as duplicates of the replay, and live delivery resumes. A no-op
     * if the stream closed in the meantime.
     */
    void completeCatchUp(ProjectId projectId, SseEmitter emitter, long caughtUpThroughSeq, @Nullable Object replayPayload) {
        Subscriber subscriber = subscriberFor(projectId, emitter);
        if (subscriber == null) {
            return;
        }
        String replayJson = (replayPayload == null) ? null : serialize(projectId, replayPayload);
        synchronized (subscriber) {
            if (replayJson != null) {
                sendEvent(projectId, subscriber, caughtUpThroughSeq, replayJson);
            }
            for (BufferedEvent buffered : subscriber.drainBuffer()) {
                if (buffered.seq() > caughtUpThroughSeq) {
                    sendEvent(projectId, subscriber, buffered.seq(), buffered.json());
                }
            }
            subscriber.resumeLive();
        }
    }

    /** Send a keep-alive comment to every open stream; failed sends evict. Package-visible for tests. */
    void heartbeat() {
        subscribersByProject.forEach((projectId, subscribers) -> subscribers.forEach(subscriber ->
                dispatchExecutor.execute(() -> sendComment(projectId, subscriber))));
    }

    private void deliverLive(ProjectId projectId, Subscriber subscriber, long sequenceId, String json) {
        synchronized (subscriber) {
            if (subscriber.isBuffering()) {
                if (subscriber.bufferSize() >= properties.getCatchUpBufferLimit()) {
                    LOGGER.debug("Catch-up buffer full for project {}; dropping live event {}, client gap detection recovers",
                                 projectId, sequenceId);
                    return;
                }
                subscriber.buffer(new BufferedEvent(sequenceId, json));
                return;
            }
            sendEvent(projectId, subscriber, sequenceId, json);
        }
    }

    /** Emit a sequence-stamped {@code project-events} frame. Caller must hold the subscriber monitor. */
    private void sendEvent(ProjectId projectId, Subscriber subscriber, long sequenceId, String json) {
        try {
            subscriber.emitter().send(SseEmitter.event()
                    .id(Long.toString(sequenceId))
                    .name(EVENT_NAME)
                    .data(json, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            LOGGER.debug("Evicting unreachable SSE subscriber for project {}: {}", projectId, e.getMessage());
            remove(projectId, subscriber);
        }
    }

    private void sendComment(ProjectId projectId, Subscriber subscriber) {
        synchronized (subscriber) {
            try {
                subscriber.emitter().send(SseEmitter.event().comment(HEARTBEAT_COMMENT));
            } catch (Exception e) {
                LOGGER.debug("Evicting unreachable SSE subscriber for project {}: {}", projectId, e.getMessage());
                remove(projectId, subscriber);
            }
        }
    }

    @Nullable
    private String serialize(ProjectId projectId, Object payload) {
        try {
            return new String(objectMapper.writeValueAsBytes(payload), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize SSE payload for project {}", projectId, e);
            return null;
        }
    }

    @Nullable
    private Subscriber subscriberFor(ProjectId projectId, SseEmitter emitter) {
        Set<Subscriber> subscribers = subscribersByProject.get(projectId);
        if (subscribers == null) {
            return null;
        }
        for (Subscriber subscriber : subscribers) {
            if (subscriber.emitter() == emitter) {
                return subscriber;
            }
        }
        return null;
    }

    private void remove(ProjectId projectId, Subscriber subscriber) {
        subscribersByProject.computeIfPresent(projectId, (key, set) -> {
            set.remove(subscriber);
            return set.isEmpty() ? null : set;
        });
    }

    int subscriberCount(ProjectId projectId) {
        Set<Subscriber> subscribers = subscribersByProject.get(projectId);
        return (subscribers == null) ? 0 : subscribers.size();
    }

    List<Subscriber> subscribersFor(ProjectId projectId) {
        Set<Subscriber> subscribers = subscribersByProject.get(projectId);
        return (subscribers == null) ? List.of() : new ArrayList<>(subscribers);
    }

    private static ThreadFactory daemonThreadFactory(String namePrefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * One open stream and the reconnect state needed to resume it. The buffer and its
     * {@code buffering} flag are only touched while holding this subscriber's monitor.
     */
    static final class Subscriber {

        private final SseEmitter emitter;

        @Nullable
        private final String lastEventId;

        private final ExecutionContext executionContext;

        private final List<BufferedEvent> buffer = new ArrayList<>();

        private boolean buffering;

        Subscriber(SseEmitter emitter, @Nullable String lastEventId, ExecutionContext executionContext) {
            this.emitter = emitter;
            this.lastEventId = lastEventId;
            this.executionContext = executionContext;
            // A reconnect carrying a lastEventId holds live events until its history replay flushes,
            // so the replay is emitted ahead of any newer live event.
            this.buffering = (lastEventId != null);
        }

        SseEmitter emitter() {
            return emitter;
        }

        @Nullable
        String lastEventId() {
            return lastEventId;
        }

        ExecutionContext executionContext() {
            return executionContext;
        }

        boolean isBuffering() {
            return buffering;
        }

        void resumeLive() {
            buffering = false;
        }

        int bufferSize() {
            return buffer.size();
        }

        void buffer(BufferedEvent event) {
            buffer.add(event);
        }

        List<BufferedEvent> drainBuffer() {
            List<BufferedEvent> drained = new ArrayList<>(buffer);
            buffer.clear();
            return drained;
        }
    }

    /** A live project-events frame held while a reconnecting stream catches up. */
    record BufferedEvent(long seq, String json) {
    }
}
