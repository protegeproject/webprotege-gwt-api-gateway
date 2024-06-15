package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.stanford.protege.webprotege.common.UserId;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-14
 */
@Component
public class RpcClient {

    private static final String PREFERRED_USERNAME = "preferred_username";

    private final RpcRequestProcessor requestProcessor;

    private final ObjectMapper objectMapper;

    public RpcClient(RpcRequestProcessor requestProcessor, ObjectMapper objectMapper) {
        this.requestProcessor = requestProcessor;
        this.objectMapper = objectMapper;
    }

    /**
     * Make a call using the specified RPC method and parameters
     * @param token The access token that is used for authentication and authorization
     * @param method The RPC method to call
     * @param params The parameters for the method
     * @return A response entity that represents the result, if successful, or an error if
     * there was a failure.
     */
    @Nonnull
    public ResponseEntity<Map<String, Object>> call(@Nonnull Jwt token,
                                                    @Nonnull RpcMethod method,
                                                    @Nonnull Map<String, Object> params) {
        var userIdClaim = token.getClaimAsString(PREFERRED_USERNAME);
        var userId = UserId.valueOf(userIdClaim);
        var paramsAsNode = serializeParams(params);
        var rpcResponse = executeCall(token, method, paramsAsNode, userId);
        var error = rpcResponse.error();
        if(error != null) {
            throw new ResponseStatusException(error.code(), error.message(), null);
        }
        return ResponseEntity.ok(rpcResponse.result());

    }

    private RpcResponse executeCall(@NotNull Jwt token, @NotNull RpcMethod method, ObjectNode paramsAsNode, UserId userId) {
        try {
            var response = requestProcessor.processRequest(new RpcRequest(method, paramsAsNode),
                                                           token.getTokenValue(), userId);
            return response.get();
        } catch (ExecutionException e) {
            throw new ResponseStatusException(500, e.getCause().getMessage(), e.getCause());
        } catch (Throwable t) {
            throw new ResponseStatusException(500, t.getMessage(), t);
        }
    }

    private ObjectNode serializeParams(Map<String, Object> params) {
        return objectMapper.convertValue(params, ObjectNode.class);
    }
}
