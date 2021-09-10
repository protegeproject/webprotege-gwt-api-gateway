package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public class RpcMethod {

    private final String methodName;

    @JsonCreator
    public RpcMethod(String methodName) {
        this.methodName = methodName;
    }

    @JsonValue
    public String getMethodName() {
        return methodName;
    }
}
