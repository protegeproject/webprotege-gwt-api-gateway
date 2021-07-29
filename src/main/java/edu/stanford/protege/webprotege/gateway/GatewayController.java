package edu.stanford.protege.webprotege.gateway;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-19
 */
@RestController
public class GatewayController {

    private final AsyncRemoteProcedureCallExecutor executor;

    public GatewayController(AsyncRemoteProcedureCallExecutor executor) {
        this.executor = executor;
    }

    @PostMapping(path = "/api/execute", consumes = "application/json")
    public RpcResponse execute(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                               @RequestBody RpcRequest request) throws ExecutionException, InterruptedException {
        var response = executor.exec(request, authorizedClient.getAccessToken());
        return response.toFuture().get();
    }

}
