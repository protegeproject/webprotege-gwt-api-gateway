package edu.stanford.protege.webprotege.gateway;

import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class MessageHandler {

    private final ReplyingKafkaTemplate<String, String, String> kafkaTemplate;

    public MessageHandler(ReplyingKafkaTemplate<String, String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends the specified {@link Message} and waits for a maximum of the duration specified by the timeout
     * @param message The message to be sent
     * @param timeout The time out that specifies the maximum duration to wait for a reply
     * @return The reply message.
     */
    public CompletableFuture<Message<?>> sendAndReceive(Message<String> message, Duration timeout) {
        var reply = kafkaTemplate.sendAndReceive(message, timeout);
        return reply.completable();
    }
}
