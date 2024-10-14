package edu.stanford.protege.webprotege.gateway.websocket.config.events;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Objects;
import edu.stanford.protege.webprotege.common.*;

import java.util.Set;

@JsonTypeName(UpdateUiHistoryEvent.CHANNEL)
public record UpdateUiHistoryEvent(EventId eventId,
                                   ProjectId projectId,
                                   Set<String> affectedEntityIris) implements ProjectEvent {

    @JsonCreator
    public static UpdateUiHistoryEvent create(@JsonProperty("eventId") EventId eventId,
                                              @JsonProperty("projectId") ProjectId projectId,
                                              @JsonProperty("afectedEntityIris") Set<String> afectedEntityIris
    ) {
        return new UpdateUiHistoryEvent(eventId, projectId, afectedEntityIris);
    }

    public static final String CHANNEL = "webprotege.events.projects.uiHistory.UpdateUiHistoryEvent";

    @JsonProperty("projectId")
    public ProjectId projectId() {
        return this.projectId;
    }

    @Override
    @JsonProperty("eventId")
    public EventId eventId() {
        return eventId;
    }

    @Override
    @JsonProperty("afectedEntityIris")
    public Set<String> affectedEntityIris() {
        return affectedEntityIris;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateUiHistoryEvent that = (UpdateUiHistoryEvent) o;
        return Objects.equal(eventId, that.eventId) && Objects.equal(projectId, that.projectId) && Objects.equal(affectedEntityIris, that.affectedEntityIris);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(eventId, projectId, affectedEntityIris);
    }

    @Override
    public String toString() {
        return "UpdateUiHistoryEvent{" +
                "eventId=" + eventId +
                ", projectId=" + projectId +
                ", subjects=" + affectedEntityIris +
                '}';
    }

}
