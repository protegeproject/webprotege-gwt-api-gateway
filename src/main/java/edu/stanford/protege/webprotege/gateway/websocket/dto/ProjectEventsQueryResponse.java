package edu.stanford.protege.webprotege.gateway.websocket.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.webprotege.common.Response;

@JsonTypeName("webprotege.hierarchies.GetProjectEvents")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME
)
public class ProjectEventsQueryResponse implements Response {
    public EventList events;
}
