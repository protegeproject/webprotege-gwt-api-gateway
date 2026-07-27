package edu.stanford.protege.webprotege.gateway.sse;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * In-memory {@link StreamTicketStore} backed by a Guava cache that evicts each ticket a fixed duration
 * after it is written ({@code expireAfterWrite}). Ticket values are 128 bits of {@link SecureRandom}
 * rendered URL-safe, so they carry safely in a query string and are infeasible to guess.
 *
 * <p>Single-instance only — a ticket lives only in the heap of the gateway that issued it. See
 * {@link StreamTicketStore} and #307.
 */
@Component
public class InMemoryStreamTicketStore implements StreamTicketStore {

    /** 16 bytes = 128 bits of entropy, per the ticket brief. */
    private static final int TICKET_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    private final Duration ttl;

    private final Cache<String, StreamTicket> tickets;

    @Autowired
    public InMemoryStreamTicketStore(SseProperties properties) {
        this(properties.getTicketTtl(), Ticker.systemTicker());
    }

    /** Package-visible so tests can drive expiry with a fake ticker instead of sleeping out the TTL. */
    InMemoryStreamTicketStore(Duration ttl, Ticker ticker) {
        this.ttl = ttl;
        this.tickets = CacheBuilder.newBuilder()
                .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
                .ticker(ticker)
                .build();
    }

    @Override
    public String issue(UserId userId, ProjectId projectId, String jwt) {
        String ticket = newTicketValue();
        tickets.put(ticket, new StreamTicket(userId, projectId, jwt, Instant.now().plus(ttl)));
        return ticket;
    }

    @Override
    public Optional<StreamTicket> redeem(String ticket) {
        if (ticket == null || ticket.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tickets.getIfPresent(ticket));
    }

    private String newTicketValue() {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
