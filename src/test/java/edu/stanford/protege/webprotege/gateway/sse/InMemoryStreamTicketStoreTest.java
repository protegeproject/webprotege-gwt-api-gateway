package edu.stanford.protege.webprotege.gateway.sse;

import com.google.common.base.Ticker;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStreamTicketStoreTest {

    private static final Duration TTL = Duration.ofSeconds(120);

    private MutableTicker ticker;

    private InMemoryStreamTicketStore store;

    private UserId user;

    private ProjectId project;

    @BeforeEach
    void setUp() {
        ticker = new MutableTicker();
        store = new InMemoryStreamTicketStore(TTL, ticker);
        user = UserId.valueOf("alice");
        project = ProjectId.generate();
    }

    @Test
    void issuedTicketRedeemsToItsRecordWithinTtl() {
        String ticket = store.issue(user, project, "jwt-token");

        Optional<StreamTicket> record = store.redeem(ticket);

        assertThat(record).isPresent();
        assertThat(record.get().userId()).isEqualTo(user);
        assertThat(record.get().projectId()).isEqualTo(project);
        assertThat(record.get().jwt()).isEqualTo("jwt-token");
    }

    @Test
    void ticketIsReusableForTheWholeTtl() {
        String ticket = store.issue(user, project, "jwt-token");

        assertThat(store.redeem(ticket)).isPresent();
        ticker.advance(Duration.ofSeconds(119));
        assertThat(store.redeem(ticket)).isPresent();
    }

    @Test
    void ticketExpiresAfterTtl() {
        String ticket = store.issue(user, project, "jwt-token");

        ticker.advance(TTL.plusSeconds(1));

        assertThat(store.redeem(ticket)).isEmpty();
    }

    @Test
    void unknownTicketIsRejected() {
        store.issue(user, project, "jwt-token");

        assertThat(store.redeem("not-a-real-ticket")).isEmpty();
    }

    @Test
    void nullOrBlankTicketIsRejected() {
        assertThat(store.redeem(null)).isEmpty();
        assertThat(store.redeem("")).isEmpty();
    }

    @Test
    void eachIssueMintsADistinctUrlSafeTicket() {
        String first = store.issue(user, project, "jwt-token");
        String second = store.issue(user, project, "jwt-token");

        assertThat(first).isNotEqualTo(second);
        // URL-safe base64 without padding, so it carries cleanly in a query string.
        assertThat(first).matches("[A-Za-z0-9_-]+");
    }

    /** A hand-advanced {@link Ticker} so expiry is exercised without waiting out the real TTL. */
    private static final class MutableTicker extends Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
