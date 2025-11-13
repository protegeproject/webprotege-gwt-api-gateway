package edu.stanford.protege.webprotege.projects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.webprotege.common.Response;


import javax.annotation.Nonnull;

@JsonTypeName("webprotege.projects.GetProjectDetails")
public record GetProjectDetailsResult(@JsonProperty("projectDetails") @Nonnull ProjectDetails projectDetails) implements Response {

    @JsonCreator
    public static GetProjectDetailsResult get(@JsonProperty("projectDetails") @Nonnull ProjectDetails projectDetails) {
        return new GetProjectDetailsResult(projectDetails);
    }
}

