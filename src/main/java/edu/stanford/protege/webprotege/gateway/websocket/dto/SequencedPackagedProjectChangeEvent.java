package edu.stanford.protege.webprotege.gateway.websocket.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A {@link PackagedProjectChangeEvent} enriched with the per-project sequence ordinal assigned to the
 * bundle when it was durably archived. Published post-persistence on {@link #CHANNEL} by the
 * event-history-service so the gateway can push it with truthful event tags
 * ({@code startTag = sequenceNumber - 1}, {@code endTag = sequenceNumber}).
 * <p>
 * This is a gateway-local mirror of the producer's event; the field names ({@code projectId},
 * {@code eventId}, {@code sequenceNumber}, {@code projectEvents}) are the wire contract and must
 * match the producer exactly.
 */
@JsonTypeName(SequencedPackagedProjectChangeEvent.CHANNEL)
public record SequencedPackagedProjectChangeEvent(ProjectId projectId, EventId eventId, int sequenceNumber, List<ProjectEvent> projectEvents) implements ProjectEvent {

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
