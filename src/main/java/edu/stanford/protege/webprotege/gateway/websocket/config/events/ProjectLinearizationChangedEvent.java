package edu.stanford.protege.webprotege.gateway.websocket.config.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.webprotege.common.*;
import org.jetbrains.annotations.NotNull;

import static edu.stanford.protege.webprotege.gateway.websocket.config.events.ProjectLinearizationChangedEvent.CHANNEL;

@JsonTypeName(CHANNEL)
public record ProjectLinearizationChangedEvent(EventId eventId,
                                               ProjectId projectId) implements ProjectEvent {
    public static final String CHANNEL = "webprotege.linearization.ProjectLinearizationChangedEvent";

    public String getChannel() {
        return CHANNEL;
    }

    public @NotNull EventId eventId() {
        return this.eventId;
    }

    public @NotNull ProjectId projectId() {
        return this.projectId;
    }

}
