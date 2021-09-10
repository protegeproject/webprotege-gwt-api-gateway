package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public record RpcResponse(@Nullable RpcError error, @Nullable Map<String, Object> result) {

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
    public static RpcResponse forResult(@Nonnull Map<String, Object> result) {
        return new RpcResponse(null, result);
    }

    @Nonnull
    public static RpcResponse forError(@Nonnull RpcError rpcError) {
        return new RpcResponse(rpcError, null);
    }
}
