package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-19
 */
@RestController
public class GatewayController {

    @Value("${webprotege.apigateway.forceUserName:}")
    private String forceUserName;

    private final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final RpcRequestProcessor rpcRequestProcessor;

    public GatewayController(RpcRequestProcessor rpcRequestProcessor) {
        this.rpcRequestProcessor = rpcRequestProcessor;
    }

    @PostMapping(path = "/api/execute", consumes = "application/json")
    public RpcResponse execute(@RequestBody RpcRequest request,
                               @AuthenticationPrincipal Jwt principal) {
        // Temp workaround for keycloak setup issues
        final String accessToken;
        final String userId;
        if (!forceUserName.isEmpty()) {
            accessToken = "BLANK";
            userId = forceUserName;
            logger.warn("Using hard coded UserId: {}", userId);
        }
        else {
            accessToken = principal.getTokenValue();
            userId = principal.getClaimAsString("preferred_username");
        }
      //  var result = rpcRequestProcessor.processRequest(request, accessToken, new UserId(userId));
        try {
            return rpcRequestProcessor.processRequestRabbit(request, accessToken, new UserId(userId));
        } catch (Exception e) {
            logger.error("Error while waiting for response to request", e);
            return RpcResponse.forError(request.methodName(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
