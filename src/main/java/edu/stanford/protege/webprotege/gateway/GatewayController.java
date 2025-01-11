package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.CommandExecutionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-19
 */
@RestController
@Lazy
public class GatewayController {


    @Value("${webprotege.apigateway.forceUserName:}")
    private String forceUserName;

    private final long timeoutInMs;

    private final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final RpcRequestProcessor rpcRequestProcessor;

    private final LogoutHandler logoutHandler;


    public GatewayController(@Value("${webprotege.gateway.timeout:600000}") long timeoutInMs, RpcRequestProcessor rpcRequestProcessor, LogoutHandler logoutHandler) {
        this.timeoutInMs = timeoutInMs;
        this.rpcRequestProcessor = rpcRequestProcessor;
        this.logoutHandler = logoutHandler;
    }

    @PostMapping(path = "/api/execute", consumes = "application/json")
    public RpcResponse execute(@RequestBody RpcRequest request,
                               @AuthenticationPrincipal Jwt principal) throws JsonProcessingException {
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
        var result = rpcRequestProcessor.processRequest(request, accessToken, new UserId(userId));
        try {
            return result.get(timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if(e.getCause() instanceof CommandExecutionException ex) {
                logger.info("Error with cause that is a CommandExecutionException.  Mapping to a ResponseStatusException: {}", ex.getStatus());
                throw new ResponseStatusException(ex.getStatus(), e.getMessage());
            }
            else if(e.getCause() instanceof ResponseStatusException ex) {
                logger.info("Error with cause that is a ResponseStatusException.  Rethrowing.");
                throw ex;
            }
            else {
                logger.error("Error while handling request.  UserId: {}  Request: {}", userId, request, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while handling the request.");
            }
        } catch (TimeoutException e) {
            logger.error("Time out in GatewayController while waiting for response to request.  Timeout set to {} ms.  Request: {}", timeoutInMs, request,  e);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "A time out occurred while waiting for the response.");
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for response.  UserId: {}  Request: {}", userId, request, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An interrupt occurred while handling the request.");
        } catch (CancellationException e) {
            logger.error("Cancellation while waiting for response.  UserId: {}  Request: {}", userId, request, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A cancellation occurred while waiting for the request.");
        }
    }

    @GetMapping(path = "/logout")
    public HttpServletResponse logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return  response;
    }
}
