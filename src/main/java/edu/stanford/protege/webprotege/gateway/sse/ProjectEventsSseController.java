package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Streams project-change events to a single viewer over a long-lived HTTP connection (server-sent events).
 *
 * <p>The connection is authorized by a short-lived, project-scoped {@code ticket} query parameter (issued by
 * {@link StreamTicketController}) rather than a bearer header, because {@code EventSource} cannot set custom
 * headers. Every connection — including each native auto-reconnect, which re-GETs the same URL — redeems the
 * ticket afresh via {@link StreamTicketService}, so {@code VIEW_PROJECT} is re-checked on every (re)connect.
 * Identity comes solely from the server-side ticket record; nothing the request carries is trusted for
 * authorization. Redemption yields 401 for a missing/unknown/expired/wrong-project ticket and 403 once the
 * ticket's identity has lost view access.
 */
@RestController
public class ProjectEventsSseController {

    static final String EVENTS_PATH = "/data/projects/{projectId}/events";

    /** nginx-specific header that disables response buffering so events flush immediately. */
    static final String X_ACCEL_BUFFERING = "X-Accel-Buffering";

    private static final String PROJECT_ID = "projectId";

    private final SseStreamRegistry registry;

    private final SseCatchUpService catchUpService;

    private final StreamTicketService ticketService;

    public ProjectEventsSseController(SseStreamRegistry registry,
                                      SseCatchUpService catchUpService,
                                      StreamTicketService ticketService) {
        this.registry = registry;
        this.catchUpService = catchUpService;
        this.ticketService = ticketService;
    }

    @GetMapping(path = EVENTS_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable(PROJECT_ID) ProjectId projectId,
                                   @RequestParam(value = "ticket", required = false) String ticket,
                                   @RequestParam(value = "lastEventId", required = false) String lastEventIdParam,
                                   @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
                                   HttpServletResponse response) {
        // Redeem before touching the stream: throws 401/403 if the ticket is not a valid, still-authorized
        // pass for this exact project. The identity/authorization context comes only from the ticket record.
        ExecutionContext executionContext = ticketService.redeem(ticket, projectId);

        // Browsers send Last-Event-ID only on automatic reconnects; fresh loads use the query param.
        String lastEventId = (lastEventIdHeader != null) ? lastEventIdHeader : lastEventIdParam;

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(X_ACCEL_BUFFERING, "no");

        SseEmitter emitter = registry.subscribe(projectId, lastEventId, executionContext);
        catchUpService.catchUp(projectId, lastEventId, emitter, executionContext);
        return emitter;
    }
}
