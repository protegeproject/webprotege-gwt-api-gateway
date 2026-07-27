package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;

/**
 * Issues short-lived, project-scoped stream tickets for the SSE endpoint.
 *
 * <p>The caller is authenticated by the resource-server filter chain: this endpoint falls under
 * {@code anyRequest().authenticated()} and is NOT {@code permitAll}, so a request without a valid bearer
 * token is rejected with 401 before it reaches this controller. Identity is read from the validated
 * {@link Jwt} exactly as {@code GatewayController} does — the request body's {@code projectId} only selects
 * the project, never the user. See {@code ProjectEventsSseController} for the matching redemption.
 */
@RestController
public class StreamTicketController {

    static final String TICKET_PATH = "/data/events/ticket";

    private final StreamTicketService ticketService;

    public StreamTicketController(StreamTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping(path = TICKET_PATH, consumes = "application/json", produces = "application/json")
    public TicketResponse issueTicket(@RequestBody TicketRequest request,
                                      @AuthenticationPrincipal Jwt principal) {
        ProjectId projectId = request.projectId();
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing projectId");
        }
        UserId userId = new UserId(principal.getClaimAsString("preferred_username"));
        String jwt = principal.getTokenValue();
        return ticketService.issueTicket(userId, projectId, jwt)
                .map(issued -> new TicketResponse(issued.ticket(), issued.expiresInSeconds()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User " + userId.id() + " does not have access to project " + projectId.id()));
    }

    /**
     * Request body. Only {@code projectId} is read; any other field — notably a client-supplied user id — is
     * ignored, because identity is taken from the validated bearer token.
     */
    public record TicketRequest(@Nullable ProjectId projectId) {
    }

    /** Response body: the opaque ticket and how long, in seconds, it stays valid. */
    public record TicketResponse(String ticket, long expiresIn) {
    }
}
