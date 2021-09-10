package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
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
                               @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient) {
        var accessToken = authorizedClient.getAccessToken().getTokenValue();
        var userId = authorizedClient.getPrincipalName();
        var result = rpcRequestProcessor.processRequest(request, accessToken, new UserId(userId));
        return Mono.fromFuture(result);
    }


}
