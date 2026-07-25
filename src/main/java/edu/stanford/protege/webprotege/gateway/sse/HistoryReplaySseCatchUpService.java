package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryRequest;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Nullable;

/**
 * Replays the events a reconnecting client missed by querying the durable event-history service.
 *
 * <p>By the time this runs the registry is already buffering live events for the stream (the
 * controller subscribes first). This service queries history for everything after {@code lastEventId},
 * hands the whole batch back to the registry as one catch-up frame stamped with the batch's end
 * sequence, and lets the registry flush the buffered live events — dropping any the replay already
 * covered — before resuming live delivery. History flattening drops per-event ordinals, so a single
 * batch replayed with a strictly-greater-than query and monotonic ids is what satisfies "nothing
 * missed, nothing duplicated".
 *
 * <p>Failure modes never kill the stream, so the client's gap detection can recover the hole:
 * a fresh connection ({@code null} id) has nothing to replay; a non-numeric id is treated as fresh
 * but still releases the buffer; a failed or empty history query resumes live-only.
 */
@Component
public class HistoryReplaySseCatchUpService implements SseCatchUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryReplaySseCatchUpService.class);

    /** Dedupe threshold that keeps every buffered event when there is nothing to replay against. */
    private static final long REPLAY_NOTHING = -1L;

    private final SseStreamRegistry registry;

    private final CommandExecutor<ProjectEventsQueryRequest, ProjectEventsQueryResponse> eventsQueryExecutor;

    public HistoryReplaySseCatchUpService(SseStreamRegistry registry,
                                          CommandExecutor<ProjectEventsQueryRequest, ProjectEventsQueryResponse> eventsQueryExecutor) {
        this.registry = registry;
        this.eventsQueryExecutor = eventsQueryExecutor;
    }

    @Override
    public void catchUp(ProjectId projectId,
                        @Nullable String lastEventId,
                        SseEmitter emitter,
                        ExecutionContext executionContext) {
        if (lastEventId == null) {
            // Fresh connection: the registry never began buffering, live delivery is already active.
            return;
        }
        Integer since = parseSequence(lastEventId);
        if (since == null) {
            // The stream is buffering (lastEventId was present) but the id is unusable: release the
            // buffer with no replay so the stream resumes live instead of stalling.
            LOGGER.warn("Ignoring non-numeric Last-Event-ID '{}' for project {}; resuming live-only", lastEventId, projectId.id());
            registry.completeCatchUp(projectId, emitter, REPLAY_NOTHING, null);
            return;
        }
        try {
            ProjectEventsQueryResponse response = queryHistory(projectId, since, executionContext);
            EventTag endTag = (response != null && response.events != null) ? response.events.endTag() : null;
            if (endTag == null) {
                LOGGER.warn("Empty history response for project {} since {}; resuming live-only", projectId.id(), since);
                registry.completeCatchUp(projectId, emitter, since, null);
                return;
            }
            registry.completeCatchUp(projectId, emitter, endTag.getOrdinal(), response);
        } catch (Exception e) {
            // Keep the stream alive; the client's gap detection recovers the missed window.
            LOGGER.error("History replay failed for project {} since {}; resuming live-only", projectId.id(), since, e);
            registry.completeCatchUp(projectId, emitter, since, null);
        }
    }

    private ProjectEventsQueryResponse queryHistory(ProjectId projectId, int since, ExecutionContext executionContext) throws Exception {
        ProjectEventsQueryRequest request = new ProjectEventsQueryRequest(projectId, EventTag.get(since));
        return eventsQueryExecutor.execute(request, executionContext).get();
    }

    @Nullable
    private static Integer parseSequence(String lastEventId) {
        String trimmed = lastEventId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
