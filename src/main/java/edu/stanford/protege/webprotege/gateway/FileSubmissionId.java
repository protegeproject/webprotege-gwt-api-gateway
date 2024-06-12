package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record FileSubmissionId(String value) {

    @JsonCreator
    public static FileSubmissionId valueOf(String value) {
        return new FileSubmissionId(value);
    }

    @JsonValue
    @Override
    public String value() {
        return value;
    }
}
