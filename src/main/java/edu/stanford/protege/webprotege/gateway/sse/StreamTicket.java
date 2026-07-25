package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;

import java.time.Instant;

/**
 * The server-side record a stream ticket resolves to: the identity and project the ticket was issued for,
 * the bearer token captured at issue time (reused as the {@code ExecutionContext} jwt when the
 * {@code VIEW_PROJECT} re-check runs on redemption), and the instant the ticket expires.
 *
 * <p>The opaque ticket value itself is deliberately NOT a field here: it is the key in
 * {@link StreamTicketStore} and must never be logged, so keeping it out of the record keeps it out of any
 * accidental {@code toString()}.
 */
public record StreamTicket(UserId userId, ProjectId projectId, String jwt, Instant expiresAt) {
}
