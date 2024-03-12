package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.CommandExecutionException;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class RpcRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Messenger messenger;
    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final TopicExchange topicExchange;

    public RpcRequestProcessor(Messenger messenger,
                               ObjectMapper objectMapper,
                               RabbitTemplate rabbitTemplate, TopicExchange topicExchange) {
        this.messenger = messenger;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.topicExchange = topicExchange;
    }


    public RpcResponse processRequestRabbit(RpcRequest request,
                                            String accessToken,
                                            UserId userId) {
        Message rabbitRequest = MessageBuilder.withBody(writePayloadForRequest(request)).build();
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.ACCESS_TOKEN, accessToken.toString());
        rabbitRequest.getMessageProperties().getHeaders().put("webprotege_methodName", request.methodName());
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.USER_ID, userId.value());

        Message response = rabbitTemplate.sendAndReceive(topicExchange.getName(), request.methodName() ,rabbitRequest);

        var result = parseResultFromResponseMessagePayload(response.getBody());

        return RpcResponse.forResult(request.methodName(), result);
    }

    /**
     * Process the {@link RpcRequest}. This is a blocking method, intended for use
     * by a Rest Controller.
     * @param request The {@link RpcRequest} to handle
     * @param accessToken A JWT access token that identifies the principle
     * @param userId The userId that corresponds to the principal
     * @return The {@link RpcResponse} that corresponds to the message that was received in response to the
     * message that was sent
     */
    public CompletableFuture<RpcResponse> processRequest(RpcRequest request,
                                                         String accessToken,
                                                         UserId userId) {
        try {
            var payload = writePayloadForRequest(request);
            var reply = messenger.sendAndReceive(request.methodName(),
                                                 accessToken,
                                                 payload, userId);

            return reply.handleAsync((replyMsg, error) -> {
                if(error != null) {
                    return createErrorResponse(request.methodName(), error);
                }
                var errorHeader = replyMsg.headers().get(Headers.ERROR);
                if (errorHeader != null) {
                    try {
                        var executionException = objectMapper.readValue(errorHeader, CommandExecutionException.class);
                        return createRpcResponse(request.methodName(), executionException.getStatus());
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing error response into ", e);
                        return createRpcResponse(request.methodName(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

                var result = parseResultFromResponseMessagePayload(replyMsg.payload());
                return RpcResponse.forResult(request.methodName(), result);
            });
        } catch (Exception e) {
            // Note: Catches InterruptedException
            return createRpcResponseFuture(request.methodName(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static CompletableFuture<RpcResponse> createRpcResponseFuture(String method, HttpStatus httpStatus) {
        var responseFuture = new CompletableFuture<RpcResponse>();
        RpcResponse response = createRpcResponse(method, httpStatus);
        responseFuture.complete(response);
        return responseFuture;
    }

    private static RpcResponse createRpcResponse(String method, HttpStatus httpStatus) {
        return new RpcResponse(method,
                               new RpcError(httpStatus.value(),
                                            httpStatus.getReasonPhrase(), Collections.emptyMap()),
                               null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResultFromResponseMessagePayload(byte [] replyPayload) {
        try {
            if(replyPayload.length == 0) {
                return Map.of();
            }
            return (Map<String, Object>) objectMapper.readValue(replyPayload, Map.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deserializing payload from response message", e);
        }
    }

    private RpcResponse createErrorResponse(String method, Throwable e) {
        if (e instanceof CommandExecutionException) {
            var status = ((CommandExecutionException) e).getStatus();
            return RpcResponse.forError(method, new RpcError(status.value(), status.getReasonPhrase(), Collections.emptyMap()));
        }
        else if(e instanceof TimeoutException) {
            return createRpcResponse(method, HttpStatus.GATEWAY_TIMEOUT);
        }
        else {
            return createRpcResponse(method, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte [] writePayloadForRequest(RpcRequest request) {
        try {
            return objectMapper.writer().writeValueAsBytes(request.params());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
