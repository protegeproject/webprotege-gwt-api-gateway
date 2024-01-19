package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import edu.stanford.protege.webprotege.ipc.pulsar.PulsarProducersManager;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static edu.stanford.protege.webprotege.ipc.pulsar.PulsarNamespaces.COMMAND_REQUESTS;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class MessengerPulsarImpl implements Messenger {

    private static final Logger logger = LoggerFactory.getLogger(MessengerPulsarImpl.class);

    private final Map<String, CompletableFuture<Msg>> replyHandlers = new ConcurrentHashMap<>();

    @Value("${webprotege.pulsar.tenant}")
    private String tenant;

    @Value("${spring.application.name}")
    private String applicationName;

    private String replyChannel;

    private final PulsarClient pulsarClient;

    private final PulsarProducersManager producersManager;

    private final ObjectMapper objectMapper;

    private Consumer<byte []> consumer;



    public MessengerPulsarImpl(PulsarClient pulsarClient,
                               PulsarProducersManager producersManager, ObjectMapper objectMapper) {
        this.pulsarClient = pulsarClient;
        this.producersManager = producersManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the specified {@link Message} and waits for a maximum of the duration specified by the timeout
     * @return The reply message.
     */
    @Override
    public CompletableFuture<Msg> sendAndReceive(String topicName, byte[] payload, UserId userId) {
        try {
            var producer = getProducer(topicName);
            var correlationId = UUID.randomUUID().toString();
            var replyFuture = new CompletableFuture<Msg>();
            replyHandlers.put(correlationId, replyFuture);
            var messageBuilder = producer.newMessage()
                                         .value(payload)
                                         .property(Headers.CORRELATION_ID, correlationId)
                                         .property(Headers.REPLY_CHANNEL, replyChannel)
                                         .property(Headers.USER_ID, userId.value());
            // TODO: FIX THIS
            //                if (request instanceof ProjectRequest) {
            //                    var projectId = ((ProjectRequest<?>) request).projectId().id();
            //                    messageBuilder.property(Headers.PROJECT_ID, projectId);
            //                    messageBuilder.key(projectId);
            //                }
            messageBuilder.send();
            return replyFuture;
        } catch (PulsarClientException e) {
            e.printStackTrace();
            return new CompletableFuture<>();
        }
    }

    private synchronized Producer<byte[]> getProducer(String topicName) {
        ensureConsumerIsListeningForReplyMessage();
        var topicUrl = "persistent://" + tenant + "/" + COMMAND_REQUESTS + "/" + topicName;
        return producersManager.getProducer(topicUrl, builder -> builder.accessMode(ProducerAccessMode.Shared));
    }

    private void ensureConsumerIsListeningForReplyMessage() {
        try {
            if (consumer != null) {
                return;
            }
            var replyTopic = "persistent://" + tenant + "/webprotege-api-gateway/" + replyChannel;

            // Replies need to go to all instances of our application/service.  In other words we have a pub/sub
            // situation.  In this case we need unique subscription names with exclusive subscriptions for each
            // consumer.
            String replySubscriptionName = applicationName + "--" + replyChannel + "--" + UUID.randomUUID();
            logger.info("Setting up consumer with subscription {} to listen for replies at {}", replySubscriptionName,
                        replyTopic);
            consumer = pulsarClient.newConsumer()
                                   .subscriptionName(replySubscriptionName)
                                   .subscriptionType(SubscriptionType.Exclusive)
                                   .topic(replyTopic)
                                   .messageListener(this::handleReplyMessageReceived)
                                   .subscribe();
        } catch (PulsarClientException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handleReplyMessageReceived(Consumer<byte[]> consumer, Message<byte[]> msg) {
        var correlationId = msg.getProperty(Headers.CORRELATION_ID);
        if (correlationId == null) {
            logger.info("CorrelationId in reply message is missing.  Cannot handle reply.  Ignoring reply.");
            return;
        }
        var replyHandler = replyHandlers.remove(correlationId);
        if(replyHandler == null) {
            // This can happen if there are multiple instances of a service running.  Only the
            // service that handled the original request can handle the reply
            return;
        }
        try {
                consumer.acknowledge(msg);
                var m = new Msg(msg.getData(), msg.getProperties());
                replyHandler.complete(m);
        } catch (PulsarClientException e) {
            logger.error("Encountered Pulsar Client Exception", e);
            replyHandler.completeExceptionally(e);
        }
    }
}
