package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.authorization.ProjectResource;
import edu.stanford.protege.webprotege.authorization.Subject;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.gateway.websocket.AccessManager;
import edu.stanford.protege.webprotege.gateway.websocket.dto.BuiltInCapability;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Streams project-change events to a single viewer over a long-lived HTTP connection
 * (server-sent events).
 *
 * <p>Interim authorization mirrors the STOMP {@code ProjectEventsInterceptor}: the caller passes
 * {@code userId} and {@code token} query params and must hold {@code VIEW_PROJECT}. Identity
 * resolution is isolated in {@link #resolveExecutionContext} so #305 can swap it for a short-lived
 * redeemable ticket.
 */
@RestController
public class ProjectEventsSseController {

    static final String EVENTS_PATH = "/data/projects/{projectId}/events";

    /** nginx-specific header that disables response buffering so events flush immediately. */
    static final String X_ACCEL_BUFFERING = "X-Accel-Buffering";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEventsSseController.class);

    private static final String PROJECT_ID = "projectId";

    private final SseStreamRegistry registry;

    private final SseCatchUpService catchUpService;

    private final AccessManager accessManager;

    public ProjectEventsSseController(SseStreamRegistry registry,
                                      SseCatchUpService catchUpService,
                                      AccessManager accessManager) {
        this.registry = registry;
        this.catchUpService = catchUpService;
        this.accessManager = accessManager;
    }

    @GetMapping(path = EVENTS_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable(PROJECT_ID) ProjectId projectId,
                                   @RequestParam("userId") String userId,
                                   @RequestParam("token") String token,
                                   @RequestParam(value = "lastEventId", required = false) String lastEventIdParam,
                                   @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
                                   HttpServletResponse response) {
        ExecutionContext executionContext = resolveExecutionContext(userId, token);
        if (!accessManager.hasPermission(Subject.forUser(userId),
                                         ProjectResource.forProject(projectId),
                                         BuiltInCapability.VIEW_PROJECT,
                                         executionContext)) {
            LOGGER.info("Denied SSE stream: user {} lacks VIEW_PROJECT on project {}", userId, projectId.id());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "User " + userId + " does not have access to project " + projectId.id());
        }

        // Browsers send Last-Event-ID only on automatic reconnects; fresh loads use the query param.
        String lastEventId = (lastEventIdHeader != null) ? lastEventIdHeader : lastEventIdParam;

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(X_ACCEL_BUFFERING, "no");

        SseEmitter emitter = registry.subscribe(projectId, lastEventId, executionContext);
        catchUpService.catchUp(projectId, lastEventId, emitter, executionContext);
        return emitter;
    }

    /**
     * Resolve the identity/authorization context from the connection's credentials. #305 replaces
     * this interim query-param scheme with a redeemable, project-scoped ticket.
     */
    private ExecutionContext resolveExecutionContext(String userId, @Nullable String token) {
        return new ExecutionContext(UserId.valueOf(userId), token, UUID.randomUUID().toString());
    }
}
