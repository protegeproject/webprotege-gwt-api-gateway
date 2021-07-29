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
public record RpcResponse(String correlationId, @Nullable RpcError error, @Nullable Map<String, Object> result) {

    public RpcResponse {
        Objects.requireNonNull(correlationId);
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
    public static RpcResponse forResult(@Nonnull String correlationId,
                                        @Nonnull Map<String, Object> result) {
        return new RpcResponse(correlationId, null, result);
    }

    @Nonnull
    public static RpcResponse forError(@Nonnull String correlationId,
                                       @Nonnull RpcError rpcError) {
        return new RpcResponse(correlationId, rpcError, null);
    }
}
