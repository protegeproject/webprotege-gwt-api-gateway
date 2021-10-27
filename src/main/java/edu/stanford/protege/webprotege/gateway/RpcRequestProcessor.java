package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.CommandExecutionException;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class RpcRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final MessageHandler messageHandler;

    private final ObjectMapper objectMapper;

    private final String replyChannel;

    private final Duration replyTimeout;

    public RpcRequestProcessor(MessageHandler messageHandler,
                               ObjectMapper objectMapper,
                               @Value("${webprotege.reply-channel}") String replyChannel, Duration replyTimeout) {
        this.messageHandler = messageHandler;
        this.objectMapper = objectMapper;
        this.replyChannel = replyChannel;
        this.replyTimeout = replyTimeout;
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
            var requestMessage = createRequestMessage(request, accessToken, userId, payload);
            var reply = messageHandler.sendAndReceive(requestMessage, replyTimeout);

            return reply.<RpcResponse>handleAsync((responseMessage, error) -> {
                if(error != null) {
                    return createErrorResponse(error);
                }
                var errorStatus = extractErrorStatus(responseMessage);
                if(errorStatus.isPresent()) {
                    return createRpcResponse(errorStatus.get());
                }
                var replyPayload = (String) responseMessage.getPayload();
                var result = parseResultFromResponseMessagePayload(replyPayload);
                return RpcResponse.forResult(result);
            });
        } catch (Exception e) {
            // Note: Catches InterruptedException
            return createRpcResponseFuture(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static CompletableFuture<RpcResponse> createRpcResponseFuture(HttpStatus httpStatus) {
        var responseFuture = new CompletableFuture<RpcResponse>();
        RpcResponse response = createRpcResponse(httpStatus);
        responseFuture.complete(response);
        return responseFuture;
    }

    private static RpcResponse createRpcResponse(HttpStatus httpStatus) {
        return new RpcResponse(new RpcError(httpStatus.value(),
                                            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), Collections.emptyMap()),
                               null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResultFromResponseMessagePayload(String replyPayload) {
        try {
            return (Map<String, Object>) objectMapper.readValue(replyPayload, Map.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deserializing payload from response message", e);
        }
    }

    /**
     * Check for an error header and deserialize it if it is present.  If an error is occurred when deserializing
     * the error header value then a {@link ResponseStatusException} is thrown with an error code of 500 (Internal
     * Server Error) and the problem is logged
     * @param message The message that could contain an error header
     * @throws ResponseStatusException if an error header is present
     */
    private Optional<HttpStatus> extractErrorStatus(Message<?> message) {
        var errorHeader = (byte [])  message.getHeaders().get(Headers.ERROR);
        if (errorHeader == null) {
            return Optional.empty();
        }
        // Deserialize the error
        try {
            var error = objectMapper.readValue(errorHeader, CommandExecutionException.class);
            return Optional.of(error.getStatus());
        } catch (IOException e) {
            logger.error("Error deserializing CommandExecutionException from error header in message", e);
            return Optional.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Message<String> createRequestMessage(RpcRequest request, String accessToken, UserId userId, String payload) {
        return MessageBuilder.withPayload(payload)
                                .setHeader(KafkaHeaders.TOPIC, request.methodName())
                                .setHeader(KafkaHeaders.REPLY_TOPIC, replyChannel)
                                .setHeader(KafkaHeaders.CORRELATION_ID, createCorrelationId())
                                                                .setHeader(Headers.ACCESS_TOKEN, accessToken)
                                .setHeader(Headers.USER_ID, userId.id())
                                .build();
    }

    private RpcResponse createErrorResponse(Throwable e) {
        if (e instanceof CommandExecutionException) {
            var status = ((CommandExecutionException) e).getStatus();
            return RpcResponse.forError(new RpcError(status.value(), status.getReasonPhrase(), Collections.emptyMap()));
        }
        else if(e instanceof KafkaReplyTimeoutException) {
            return createRpcResponse(HttpStatus.GATEWAY_TIMEOUT);
        }
        else {
            return createRpcResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String writePayloadForRequest(RpcRequest request) {
        try {
            return objectMapper.writer().writeValueAsString(request.params());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static String createCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
