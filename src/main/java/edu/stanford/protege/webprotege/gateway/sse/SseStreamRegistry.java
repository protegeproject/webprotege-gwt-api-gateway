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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

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
import java.util.function.Supplier;

/**
 * Holds the open server-sent event streams, keyed by project, and fans project-change events out
 * to every stream watching that project.
 *
 * <p>Delivery never happens on the caller's thread: {@link #publish} hands each send to a dispatch
 * executor, and the heartbeat scheduler enqueues its keep-alives on the same executor. Writes to a
 * single emitter are serialized on the subscriber monitor so concurrent live sends and heartbeats
 * cannot interleave and corrupt the stream. Any failed send evicts the dead emitter.
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
     * @param lastEventId      the last event id the client already has (stored for reconnect
     *                         catch-up), or {@code null} for a fresh connection.
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
     * executor.
     */
    public void publish(ProjectId projectId, long sequenceId, Object payload) {
        Set<Subscriber> subscribers = subscribersByProject.get(projectId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        String json;
        try {
            json = new String(objectMapper.writeValueAsBytes(payload), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize SSE payload for project {}", projectId, e);
            return;
        }
        String id = Long.toString(sequenceId);
        for (Subscriber subscriber : subscribers) {
            dispatchExecutor.execute(() -> deliver(projectId, subscriber, () -> SseEmitter.event()
                    .id(id)
                    .name(EVENT_NAME)
                    .data(json, MediaType.APPLICATION_JSON)));
        }
    }

    /** Send a keep-alive comment to every open stream; failed sends evict. Package-visible for tests. */
    void heartbeat() {
        subscribersByProject.forEach((projectId, subscribers) -> subscribers.forEach(subscriber ->
                dispatchExecutor.execute(() -> deliver(projectId, subscriber,
                        () -> SseEmitter.event().comment(HEARTBEAT_COMMENT)))));
    }

    private void deliver(ProjectId projectId, Subscriber subscriber, Supplier<SseEventBuilder> event) {
        synchronized (subscriber) {
            try {
                subscriber.emitter().send(event.get());
            } catch (Exception e) {
                LOGGER.debug("Evicting unreachable SSE subscriber for project {}: {}", projectId, e.getMessage());
                remove(projectId, subscriber);
            }
        }
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

    /** One open stream and the reconnect state needed to resume it. */
    record Subscriber(SseEmitter emitter, @Nullable String lastEventId, ExecutionContext executionContext) {
    }
}
