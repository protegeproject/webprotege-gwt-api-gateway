package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

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
        try {
            result.get();
            System.out.println(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Mono.fromFuture(result);
    }


}
