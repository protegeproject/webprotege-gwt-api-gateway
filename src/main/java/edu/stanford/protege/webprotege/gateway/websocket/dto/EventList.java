package edu.stanford.protege.webprotege.gateway.websocket.dto;

import com.fasterxml.jackson.databind.JsonNode;
import edu.stanford.protege.webprotege.event.EventTag;

/**
 * The window of events the client receives. The gateway only relays the events — it never acts on
 * their contents — so they are carried as raw JSON rather than typed objects: the producers (the
 * backend and the event-history service) and the browser client agree on the event payloads, and
 * forcing them through this service's own event model just makes delivery fail whenever the
 * gateway's model drifts from the producer's serialization.
 */
public record EventList(EventTag startTag, JsonNode events, EventTag endTag) {
}
