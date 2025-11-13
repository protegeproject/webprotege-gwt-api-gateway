package edu.stanford.protege.webprotege.projects;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.protege.webprotege.common.DictionaryLanguage;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.lang.DisplayNameSettings;
import edu.stanford.protege.webprotege.projectsettings.EntityDeprecationSettings;

import javax.annotation.Nonnull;
import java.time.Instant;

public record ProjectDetails(
        @JsonProperty("_id") @Nonnull ProjectId projectId,
        @JsonProperty("displayName") @Nonnull String displayName,
        @JsonProperty("description") @Nonnull String description,
        @JsonProperty("owner") @Nonnull UserId owner,
        @JsonProperty("inTrash") boolean inTrash,
        @JsonProperty("defaultLanguage") @Nonnull DictionaryLanguage dictionaryLanguage,
        @JsonProperty("defaultDisplayNameSettings") @Nonnull DisplayNameSettings displayNameSettings,
        @JsonProperty("createdAt") @Nonnull Instant createdAt,
        @JsonProperty("createdBy") @Nonnull UserId createdBy,
        @JsonProperty("modifiedAt") @Nonnull Instant lastModifiedAt,
        @JsonProperty("modifiedBy") @Nonnull UserId lastModifiedBy,
        @JsonProperty("entityDeprecationSettings") @Nonnull EntityDeprecationSettings entityDeprecationSettings,
        @JsonProperty("underMaintenance") boolean underMaintenance
) {
    /**
     * Gets the timestamp of when the project was created as milliseconds since epoch.
     */
    public long getCreatedAt() {
        return createdAt.toEpochMilli();
    }

    /**
     * Gets the timestamp of when the project was last modified as milliseconds since epoch.
     */
    public long getLastModifiedAt() {
        return lastModifiedAt.toEpochMilli();
    }
}
