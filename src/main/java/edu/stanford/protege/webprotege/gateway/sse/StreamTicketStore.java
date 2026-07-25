package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;

import java.util.Optional;

/**
 * Issues and redeems short-lived, project-scoped stream tickets that authorize a single SSE stream
 * connection. A ticket is an opaque, high-entropy value handed to an already-authenticated caller and later
 * presented as a query parameter by the browser's {@code EventSource} (which cannot send an
 * {@code Authorization} header). Redemption resolves the ticket back to the server-side identity so the
 * {@code VIEW_PROJECT} check can be re-run without trusting anything the client supplies.
 *
 * <p>A ticket is reusable for the whole of its TTL: the browser's native auto-reconnect re-GETs the same
 * stream URL with the same ticket, and every such reconnect must redeem successfully.
 *
 * <p><b>#307:</b> the in-memory implementation binds a ticket to the single gateway instance that issued it,
 * so a reconnect that load-balances onto a different instance would fail redemption. Multi-instance
 * deployments must replace it with a shared or stateless (e.g. HMAC-signed, self-describing) store.
 */
public interface StreamTicketStore {

    /**
     * Issue a new ticket bound to {@code userId}, {@code projectId} and {@code jwt}, valid for the
     * configured TTL. The returned value is the opaque ticket the caller hands back on the stream URL;
     * never log it.
     */
    String issue(UserId userId, ProjectId projectId, String jwt);

    /**
     * Resolve a ticket to its server-side record. Returns empty when the ticket is unknown or has expired.
     * A returned record still has to have its {@code projectId} matched against the stream being opened; the
     * store does not know which stream the caller is redeeming against.
     */
    Optional<StreamTicket> redeem(String ticket);
}
