package edu.stanford.protege.webprotege.projects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.ProjectRequest;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonTypeName("webprotege.projects.GetProjectDetails")
public record GetProjectDetailsAction(ProjectId projectId) implements ProjectRequest<GetProjectDetailsResult> {

    public static final String CHANNEL = "webprotege.projects.GetProjectDetails";

    @JsonCreator
    public GetProjectDetailsAction(@JsonProperty("projectId") ProjectId projectId) {
        this.projectId = checkNotNull(projectId);
    }

    @Nonnull
    @Override
    public ProjectId projectId() {
        return projectId;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}

