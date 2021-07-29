package edu.stanford.protege.webprotege.gateway;

import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-22
 */
public record RpcError(int code,
                       String message,
                       Map<String, Object> data) {
}
