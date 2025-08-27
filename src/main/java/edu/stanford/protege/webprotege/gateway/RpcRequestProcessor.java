package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.CommandExecutionException;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.core.AmqpMessageReturnedException;
import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class RpcRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final String applicationName;

    private final Messenger messenger;

    private final ObjectMapper objectMapper;


    public RpcRequestProcessor(@Value("${spring.application.name}") String applicationName,
                               Messenger messenger,
                               ObjectMapper objectMapper) {
        this.applicationName = applicationName;
        this.messenger = messenger;
        this.objectMapper = objectMapper;
    }

    /**
     * Process the {@link RpcRequest}.  Note that application level errors are returns as RPC error responses while
     * all other errors (for example transport level errors, messaging errors etc. are returned as HTTP errors).
     *
     * @param request     The {@link RpcRequest} to handle
     * @param accessToken A JWT access token that identifies the principle
     * @param userId      The userId that corresponds to the principal
     * @return The {@link RpcResponse} that corresponds to the message that was received in response to the
     * message that was sent.  A failed future will be returned if the request could not be processed, for whatever
     * reason.  Failed futures will have a {@link ResponseStatusException} as the error with a status code of
     * 500 Internal Server Error.
     */
    public CompletableFuture<RpcResponse> processRequest(RpcRequest request,
                                                         String accessToken,
                                                         UserId userId) {
        try {
            // Write out the payload and send it
            var payload = objectMapper.writer().writeValueAsBytes(request.params());
            return sendMessage(request, accessToken, userId, payload);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing RPC request payload: {}.  Returning failed future with ResponseStatusException HTTP 500 Internal Server Error.", e.getMessage(), e);
            return CompletableFuture.failedFuture(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e));
        }
    }

    private CompletableFuture<RpcResponse> sendMessage(RpcRequest request, String accessToken, UserId userId, byte[] payload) {
        try {
            var reply = messenger.sendAndReceive(request,
                    accessToken,
                    payload, userId);
            return reply
                    .exceptionally(e -> {
                        if(e instanceof CompletionException completionException) {
                            if(completionException.getCause() instanceof AmqpReplyTimeoutException timeoutException) {
                                logger.error("Timeout while waiting for reply to message on channel {}: {}", request.methodName(), timeoutException.getMessage(), e);
                                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Timed out while waiting for reply to message on channel " + request.methodName(), timeoutException);
                            }
                            else if(completionException.getCause() instanceof AmqpMessageReturnedException messageReturnedException) {
                                logger.error("Message returned: {}", messageReturnedException.getMessage(), e);
                                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Message to channel " + request.methodName() + " was returned");
                            }
                        }
                        logger.error("Error during send and receive: {}.  Returning failed future with ResponseStatusException HTTP 500 Internal Server Error", e.getMessage(), e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);

                    })
                    .thenCompose(msg -> {
                        try {

                            var rpcResponse = convertReplyMessageToRpcResponse(request, msg);
                            return CompletableFuture.completedFuture(rpcResponse);
                        } catch (IOException e) {
                            logger.error("Error parsing RPC response message: {}.  Returning failed future with HTTP 500 Internal Server Error.", e.getMessage(), e);
                            return CompletableFuture.failedFuture(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e));
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e));
        }
    }

    private @NotNull RpcResponse convertReplyMessageToRpcResponse(RpcRequest request, Msg replyMsg) throws IOException {
        var errorHeader = replyMsg.headers().get(Headers.ERROR);
        if (errorHeader != null) {
            var serviceName = replyMsg.headers().get("webprotege_serviceName");
            return createRpcErrorResponseFromMessageErrorHeader(request, errorHeader, serviceName);
        } else {
            var result = parseResultFromResponseMessagePayload(replyMsg.payload());
            return RpcResponse.forResult(request.methodName(), result);
        }
    }

    private @NotNull RpcResponse createRpcErrorResponseFromMessageErrorHeader(RpcRequest request, String errorHeader, @Nullable String serviceName) throws JsonProcessingException {
        try {
            var executionException = objectMapper.readValue(errorHeader, CommandExecutionException.class);
            var httpStatus = executionException.getStatus();
            var errorMessage = httpStatus.getReasonPhrase() + " in " + applicationName;
            return RpcResponse.forError(request.methodName(),
                    new RpcError(httpStatus.value(),
                            errorMessage, Collections.emptyMap()));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing error header {} into CommandExecutionException: {}", errorHeader, e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResultFromResponseMessagePayload(byte[] replyPayload) throws IOException {
        if (replyPayload.length == 0) {
            return Map.of();
        }
        try {
            return (Map<String, Object>) objectMapper.readValue(replyPayload, Map.class);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing payload into Map: {}", e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            logger.error("Error processing payload: {}", e.getMessage(), e);
            throw e;
        }
    }
}
