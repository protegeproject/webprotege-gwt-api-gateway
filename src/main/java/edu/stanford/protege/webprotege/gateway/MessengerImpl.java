package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static edu.stanford.protege.webprotege.gateway.RabbitClientConfiguration.RPC_EXCHANGE;
import static edu.stanford.protege.webprotege.gateway.RabbitClientConfiguration.RPC_QUEUE1;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class MessengerImpl implements Messenger {


    private final RabbitTemplate rabbitTemplate;



    public MessengerImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends the payload to an exchange and uses the topicName as a routing key and waits for a maximum of the duration specified by the timeout
     * @return The reply message.
     */
    @Override
    public CompletableFuture<Msg> sendAndReceive(String topicName,String accessToken, byte[] payload, UserId userId) {
        org.springframework.amqp.core.Message rabbitRequest = MessageBuilder.withBody(payload).build();
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.ACCESS_TOKEN, accessToken.toString());
        rabbitRequest.getMessageProperties().getHeaders().put("webprotege_methodName", topicName);
        rabbitRequest.getMessageProperties().getHeaders().put(Headers.USER_ID, userId.value());

        org.springframework.amqp.core.Message response = rabbitTemplate.sendAndReceive(RPC_QUEUE1, RPC_EXCHANGE ,rabbitRequest);

        Map<String, String> responseHeaders = new HashMap<>();
        if(response != null) {
            response.getMessageProperties().getHeaders().forEach((key, value) -> responseHeaders.put(key, String.valueOf(value)));
            return CompletableFuture.completedFuture(new Msg(response.getBody(), responseHeaders));
        }

        return new CompletableFuture<>();
    }


}
