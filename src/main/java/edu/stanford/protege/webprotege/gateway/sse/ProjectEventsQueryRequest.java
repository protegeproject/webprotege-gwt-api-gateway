package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Request;
import edu.stanford.protege.webprotege.event.EventTag;

/**
 * Gateway-local mirror of the event-history-service's project-events query request. The SSE catch-up
 * path sends this on {@link #CHANNEL} with {@code sinceTag} = the client's last-seen sequence to pull
 * everything persisted after it.
 * <p>
 * The field names ({@code sinceTag}, {@code projectId}) are the wire contract and must match the
 * producer's request exactly; the reply comes back as {@link ProjectEventsQueryResponse}.
 */
public class ProjectEventsQueryRequest implements Request<ProjectEventsQueryResponse> {

    public final static String CHANNEL = "webprotege.hierarchies.GetProjectEvents";

    public EventTag sinceTag;

    public ProjectId projectId;

    public ProjectEventsQueryRequest() {
    }

    public ProjectEventsQueryRequest(ProjectId projectId, EventTag sinceTag) {
        this.projectId = projectId;
        this.sinceTag = sinceTag;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
