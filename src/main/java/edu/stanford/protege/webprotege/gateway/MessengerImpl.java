package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static edu.stanford.protege.webprotege.gateway.RabbitClientConfiguration.RPC_EXCHANGE;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class MessengerImpl implements Messenger {


    private final AsyncRabbitTemplate asyncRabbitTemplate;



    public MessengerImpl(AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }

    /**
     * Sends the payload to an exchange and uses the topicName as a routing key and waits for a maximum of the duration specified by the timeout
     * @return The reply message.
     */
    @Override
    public CompletableFuture<Msg> sendAndReceive(RpcRequest rpcRequest,String accessToken, byte[] payload, UserId userId) {
        String topicName = rpcRequest.methodName();
        org.springframework.amqp.core.Message rabbitRequest = MessageBuilder.withBody(payload).build();
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.ACCESS_TOKEN, accessToken.toString());
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.METHOD, topicName);
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.USER_ID, userId.value());
        if(rpcRequest.params().has("projectId")) {
            var projectId = rpcRequest.params().get("projectId").asText();
            rabbitRequest.getMessageProperties().getHeaders().put(Headers.PROJECT_ID, projectId);
        }
        return asyncRabbitTemplate.sendAndReceive(RPC_EXCHANGE, topicName, rabbitRequest).thenApply(response -> {
            Map<String, String> responseHeaders = new HashMap<>();
            if(response != null) {
                response.getMessageProperties().getHeaders().forEach((key, value) -> responseHeaders.put(key, String.valueOf(value)));
                return new Msg(response.getBody(), responseHeaders);
            } else {
                throw new RuntimeException("Null message received from Rabbit ");
            }
        });

    }


}
