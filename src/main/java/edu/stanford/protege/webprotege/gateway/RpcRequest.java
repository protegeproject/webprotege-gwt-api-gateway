package edu.stanford.protege.webprotege.gateway;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public record RpcRequest(RpcMethod method, ObjectNode parameters) {

    String methodName() {
        return this.method.getMethodName();
    }
}
