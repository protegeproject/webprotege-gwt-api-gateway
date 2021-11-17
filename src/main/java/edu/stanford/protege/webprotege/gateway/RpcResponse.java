package edu.stanford.protege.webprotege.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public record RpcResponse(String method, @Nullable RpcError error, @Nullable Map<String, Object> result) {

    public RpcResponse {
        if(error == null) {
            if(result == null) {
                throw new IllegalArgumentException("One of error or result MUST NOT be null");
            }
        }
        else {
            if(result != null) {
                throw new IllegalArgumentException("One of error or result MUST be null");
            }
        }
    }

    @Nonnull
    public static RpcResponse forResult(String method, @Nonnull Map<String, Object> result) {
        return new RpcResponse(method, null, result);
    }

    @Nonnull
    public static RpcResponse forError(String method, @Nonnull RpcError rpcError) {
        return new RpcResponse(method, rpcError, null);
    }

    @Nonnull
    public static RpcResponse forError(String method, HttpStatus status) {
        return RpcResponse.forError(method, RpcError.forStatus(status));
    }
}
