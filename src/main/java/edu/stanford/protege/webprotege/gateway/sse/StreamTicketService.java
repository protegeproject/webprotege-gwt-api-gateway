package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.authorization.ProjectResource;
import edu.stanford.protege.webprotege.authorization.Subject;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.gateway.websocket.AccessManager;
import edu.stanford.protege.webprotege.gateway.websocket.dto.BuiltInCapability;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Issues and redeems SSE stream tickets, enforcing {@code VIEW_PROJECT} on both paths.
 *
 * <p>The security property the ticket buys: at issue time the caller is a fully authenticated bearer
 * principal, and at redemption time identity comes ONLY from the server-side ticket record — never from
 * anything the {@code EventSource} request carries — so a leaked query-string ticket cannot be used to
 * impersonate another user or reach another project, and permission is re-verified on every (re)connect.
 */
@Service
public class StreamTicketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTicketService.class);

    private final StreamTicketStore store;

    private final AccessManager accessManager;

    private final SseProperties properties;

    public StreamTicketService(StreamTicketStore store, AccessManager accessManager, SseProperties properties) {
        this.store = store;
        this.accessManager = accessManager;
        this.properties = properties;
    }

    /**
     * Issue a ticket for {@code userId} to stream {@code projectId}, provided that identity currently holds
     * {@code VIEW_PROJECT}. {@code jwt} is the caller's validated bearer token, captured so the redemption
     * re-check can call the authorization service as the same principal. Returns empty when the permission
     * check fails.
     */
    public Optional<IssuedTicket> issueTicket(UserId userId, ProjectId projectId, String jwt) {
        if (!hasViewProject(userId, projectId, jwt)) {
            LOGGER.info("Denied stream ticket: user {} lacks VIEW_PROJECT on project {}", userId.id(), projectId.id());
            return Optional.empty();
        }
        String ticket = store.issue(userId, projectId, jwt);
        LOGGER.info("Issued stream ticket for user {} on project {}", userId.id(), projectId.id());
        return Optional.of(new IssuedTicket(ticket, properties.getTicketTtl().toSeconds()));
    }

    /**
     * Redeem {@code ticket} to open a stream for {@code projectId}. Throws {@code 401} when the ticket is
     * missing, unknown, expired, or bound to a different project, and {@code 403} when the ticket's identity
     * has since lost {@code VIEW_PROJECT}. On success returns the identity/authorization context resolved
     * entirely from the server-side record.
     */
    public ExecutionContext redeem(String ticket, ProjectId projectId) {
        if (ticket == null || ticket.isBlank()) {
            throw unauthorized(projectId);
        }
        StreamTicket record = store.redeem(ticket).orElse(null);
        if (record == null || !record.projectId().equals(projectId)) {
            // Unknown, expired, or wrong-project ticket: no identity to attribute, and never log the ticket.
            throw unauthorized(projectId);
        }
        ExecutionContext executionContext = newExecutionContext(record.userId(), record.jwt());
        if (!accessManager.hasPermission(Subject.forUser(record.userId()),
                                         ProjectResource.forProject(projectId),
                                         BuiltInCapability.VIEW_PROJECT,
                                         executionContext)) {
            LOGGER.info("Denied SSE stream: user {} no longer has VIEW_PROJECT on project {}",
                        record.userId().id(), projectId.id());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User " + record.userId().id() + " does not have access to project " + projectId.id());
        }
        return executionContext;
    }

    private ResponseStatusException unauthorized(ProjectId projectId) {
        LOGGER.info("Rejected SSE stream: invalid or expired ticket for project {}", projectId.id());
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired stream ticket");
    }

    private boolean hasViewProject(UserId userId, ProjectId projectId, String jwt) {
        return accessManager.hasPermission(Subject.forUser(userId),
                                           ProjectResource.forProject(projectId),
                                           BuiltInCapability.VIEW_PROJECT,
                                           newExecutionContext(userId, jwt));
    }

    private ExecutionContext newExecutionContext(UserId userId, String jwt) {
        return new ExecutionContext(userId, jwt, UUID.randomUUID().toString());
    }

    /** The wire result of a successful issuance: the opaque ticket and its lifetime in seconds. */
    public record IssuedTicket(String ticket, long expiresInSeconds) {
    }
}
