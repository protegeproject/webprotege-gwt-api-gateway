package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Seam for replaying events a reconnecting client missed while it was disconnected.
 *
 * <p>The controller subscribes the emitter to live events first, then calls this service so a
 * later change can replay from the durable history (identified by {@code lastEventId}) before the
 * buffered live events flush. The live-only implementation shipped here does nothing.
 */
public interface SseCatchUpService {

    /**
     * Replay events that occurred after {@code lastEventId} onto {@code emitter}.
     *
     * @param projectId        the project whose events are streamed.
     * @param lastEventId      the last event id the client already received, or {@code null} for a
     *                         fresh connection with nothing to replay.
     * @param emitter          the already-subscribed emitter to replay onto.
     * @param executionContext the identity/authorization context resolved for this connection.
     */
    void catchUp(ProjectId projectId,
                 String lastEventId,
                 SseEmitter emitter,
                 ExecutionContext executionContext);
}
