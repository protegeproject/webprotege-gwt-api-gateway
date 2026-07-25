package edu.stanford.protege.webprotege.gateway.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tunables for the server-sent events streaming endpoint.
 *
 * <p>The heartbeat interval must stay comfortably below the nginx {@code proxy_read_timeout}
 * (60s by default) so idle streams are not dropped. The stream timeout closes long-lived
 * connections so each reconnect becomes a fresh authorization checkpoint.
 */
@ConfigurationProperties(prefix = "webprotege.sse")
public class SseProperties {

    private Duration heartbeatInterval = Duration.ofSeconds(20);

    private Duration streamTimeout = Duration.ofMinutes(30);

    /**
     * How long a stream ticket stays valid after it is issued. Short by design: a ticket travels in the
     * stream URL's query string (and so into nginx access logs and browser history), and the window bounds
     * that exposure. It must be long enough to cover a browser's native reconnect cadence, since the client
     * reuses the same ticket for the whole TTL. Configurable via {@code webprotege.sse.ticket-ttl} so tests
     * need not wait out the default.
     */
    private Duration ticketTtl = Duration.ofSeconds(120);

    /**
     * Cap on the live events a reconnecting stream buffers while its history replay is fetched.
     * Overflow drops the excess and lets the client's gap detection recover; it only guards against
     * unbounded growth if a history query stalls.
     */
    private int catchUpBufferLimit = 1000;

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getStreamTimeout() {
        return streamTimeout;
    }

    public void setStreamTimeout(Duration streamTimeout) {
        this.streamTimeout = streamTimeout;
    }

    public int getCatchUpBufferLimit() {
        return catchUpBufferLimit;
    }

    public void setCatchUpBufferLimit(int catchUpBufferLimit) {
        this.catchUpBufferLimit = catchUpBufferLimit;
    }

    public Duration getTicketTtl() {
        return ticketTtl;
    }

    public void setTicketTtl(Duration ticketTtl) {
        this.ticketTtl = ticketTtl;
    }
}
