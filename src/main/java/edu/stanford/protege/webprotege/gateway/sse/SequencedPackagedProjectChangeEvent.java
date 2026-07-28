package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

import javax.annotation.Nonnull;

/**
 * A packaged project-change bundle enriched with the per-project sequence ordinal assigned to the
 * bundle when it was durably archived. Published post-persistence on {@link #CHANNEL} by the
 * event-history-service so the gateway can push it with truthful event tags
 * ({@code startTag = sequenceNumber - 1}, {@code endTag = sequenceNumber}).
 * <p>
 * This is a gateway-local mirror of the producer's event; the field names ({@code projectId},
 * {@code eventId}, {@code sequenceNumber}, {@code projectEvents}) are the wire contract and must
 * match the producer exactly. The inner events stay raw JSON on purpose: the gateway only relays
 * them, and deserializing them into this service's own event model breaks delivery whenever that
 * model drifts from the producer's serialization.
 */
@JsonTypeName(SequencedPackagedProjectChangeEvent.CHANNEL)
public record SequencedPackagedProjectChangeEvent(ProjectId projectId, EventId eventId, int sequenceNumber, JsonNode projectEvents) implements ProjectEvent {

    public final static String CHANNEL = "webprotege.events.projects.SequencedPackagedProjectChange";


    @Nonnull
    @Override
    public ProjectId projectId() {
        return projectId;
    }

    @Nonnull
    @Override
    public EventId eventId() {
        return eventId;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
