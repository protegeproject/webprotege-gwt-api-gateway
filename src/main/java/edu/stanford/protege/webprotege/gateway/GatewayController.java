package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-19
 */
@RestController
public class GatewayController {

    private final RpcRequestProcessor rpcRequestProcessor;

    public GatewayController(RpcRequestProcessor rpcRequestProcessor) {
        this.rpcRequestProcessor = rpcRequestProcessor;
    }

    @PostMapping(path = "/api/execute", consumes = "application/json")
    public Mono<RpcResponse> execute(@RequestBody RpcRequest request,
                                     @AuthenticationPrincipal Jwt principal) {
        var accessToken = principal.getTokenValue();
        var userId = principal.getClaimAsString("preferred_username");
        var result = rpcRequestProcessor.processRequest(request, accessToken, new UserId(userId));
        return Mono.fromFuture(result);
    }
}
