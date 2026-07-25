package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Default {@link SseCatchUpService} that replays nothing: connections receive live events only.
 * Reconnecting clients resume the live stream without recovering events missed while disconnected.
 */
@Component
public class LiveOnlySseCatchUpService implements SseCatchUpService {

    @Override
    public void catchUp(ProjectId projectId,
                        String lastEventId,
                        SseEmitter emitter,
                        ExecutionContext executionContext) {
        // Live-only delivery: no history replay.
    }
}
