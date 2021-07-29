package edu.stanford.protege.webprotege.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static edu.stanford.protege.webprotege.gateway.HeaderConstants.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
@Component
public class AsyncRemoteProcedureCallExecutor {


    /*

    Things to think about:

        2) Inbound channel name
        3) Timeouts

     */


    private static final Logger logger = LoggerFactory.getLogger(AsyncRemoteProcedureCallExecutor.class);

    public static final String REPLY_CHANNEL = "reply-channel";

    private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> handlers = new ConcurrentHashMap<>();

    private final StreamBridge streamBridge;

    private final MessageIdFactory messageIdFactory;

    private final MessageChannelMapper messageChannelMapper;


    public AsyncRemoteProcedureCallExecutor(StreamBridge streamBridge,
                                            MessageIdFactory messageIdFactory,
                                            MessageChannelMapper messageChannelMapper) {
        this.streamBridge = streamBridge;
        this.messageIdFactory = messageIdFactory;
        this.messageChannelMapper = messageChannelMapper;
    }

    public Mono<RpcResponse> exec(RpcRequest request,
                                  OAuth2AccessToken client) {
        var msgId = messageIdFactory.createCorrelationId();
        var futureResponse = new CompletableFuture<RpcResponse>();
        handlers.put(msgId, futureResponse);
        var msg = MessageBuilder.withPayload(request)
                                .setHeader(MSG_ID, msgId)
                                .setHeader(REPLY_TO, REPLY_CHANNEL)
                                .setHeader(ACCESS_TOKEN, client.getTokenValue())
                                .build();
        var channel = messageChannelMapper.getChannelName(request);
        streamBridge.send(channel, msg);
        return Mono.fromFuture(futureResponse);
    }

    @Bean
    protected Consumer<Message<?>> replyHandler() {
        return msg -> {
            var correlationId = (String) msg.getHeaders().get(CORRELATION_ID);
            if (correlationId != null) {
                var payload = (RpcResponse) msg.getPayload();
                var future = handlers.remove(correlationId);
                if (future != null) {
                    future.complete(payload);
                }
                else {
                    logger.info("Received reply. No handler found.  CoId: {} Msg: {}", correlationId, msg.getPayload());
                }
            }
        };
    }

}
