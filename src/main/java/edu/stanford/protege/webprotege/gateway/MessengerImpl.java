package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.gateway.websocket.ProjectMaintenanceState;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.Headers;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-08
 */
public class MessengerImpl implements Messenger {

    private final static Logger LOGGER = LoggerFactory.getLogger(MessengerImpl.class);

    private final AsyncRabbitTemplate asyncRabbitTemplate;
    private final ProjectMaintenanceState maintenanceState;


    public MessengerImpl(AsyncRabbitTemplate asyncRabbitTemplate, ProjectMaintenanceState maintenanceState) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
        this.maintenanceState = maintenanceState;
    }

    /**
     * Sends the payload to an exchange and uses the topicName as a routing key and waits for a maximum of the duration specified by the timeout
     *
     * @return The reply message.
     */
    @Override
    public CompletableFuture<Msg> sendAndReceive(RpcRequest rpcRequest, String accessToken, byte[] payload, UserId userId) {
        var method = rpcRequest.methodName();
        var rabbitRequest = MessageBuilder.withBody(payload).build();
        var headers = rabbitRequest.getMessageProperties().getHeaders();
        headers.put(Headers.ACCESS_TOKEN, accessToken);
        headers.put(Headers.METHOD, method);
        headers.put(Headers.USER_ID, userId.value());
        headers.put(Headers.CORRELATION_ID, CorrelationMDCUtil.getCorrelationId());
        LOGGER.info("Setting correlationId : " + CorrelationMDCUtil.getCorrelationId());
        if (rpcRequest.params().has("projectId")) {
            var projectIdString = rpcRequest.params().get("projectId").asText();
            headers.put(Headers.PROJECT_ID, projectIdString);
            
            var projectId = ProjectId.valueOf(projectIdString);
            var executionContext = new ExecutionContext(userId, accessToken, UUID.randomUUID().toString());
            if (maintenanceState.isUnderMaintenance(projectId, executionContext)) {
                throw new ProjectUnderMaintenanceException(projectIdString);
            }
        }
        LOGGER.info("User {} is sending: {}",userId.value(), rpcRequest.methodName());
        return asyncRabbitTemplate.sendAndReceive("webprotege-exchange", method, rabbitRequest)
                .thenCompose(replyMsg -> {
                    // Transform the replyMsg to a Msg
                    var responseHeaders = new HashMap<String, String>();
                    if (replyMsg != null) {
                        replyMsg.getMessageProperties().getHeaders().forEach((key, value) -> responseHeaders.put(key, String.valueOf(value)));
                        var msg = new Msg(replyMsg.getBody(), responseHeaders);
                        return CompletableFuture.completedFuture(msg);
                    } else {
                        return CompletableFuture.failedFuture(new RuntimeException("Null replyMsg from Rabbit"));
                    }
                });
    }
}
