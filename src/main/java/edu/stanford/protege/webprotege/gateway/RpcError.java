package edu.stanford.protege.webprotege.gateway;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-22
 */
public record RpcError(int code,
                       String message,
                       Map<String, Object> data) {

    public static RpcError forStatus(HttpStatus status) {
        return new RpcError(status.value(), status.getReasonPhrase(), Collections.emptyMap());
    }
}
