package edu.stanford.protege.webprotege.gateway.websocket.config;

import edu.stanford.protege.webprotege.authorization.ProjectResource;
import edu.stanford.protege.webprotege.authorization.Subject;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.gateway.websocket.AccessManager;
import edu.stanford.protege.webprotege.gateway.websocket.dto.BuiltInCapability;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AuthorizationServiceException;

import java.util.List;
import java.util.UUID;

public class ProjectEventsInterceptor  implements ChannelInterceptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectEventsInterceptor.class);
    private final AccessManager accessManager;

    public ProjectEventsInterceptor(AccessManager accessManager) {
        this.accessManager = accessManager;
    }


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Only check authorization for SUBSCRIBE messages
        // getMessageType() can be null for heartbeat messages, non-STOMP frames, or internal messages
        if(SimpMessageType.SUBSCRIBE.equals(accessor.getMessageType())) {
            String destination = accessor.getDestination();
            if(destination == null || destination.isEmpty()) {
                LOGGER.error("Missing destination in SUBSCRIBE message");
                throw new AuthorizationServiceException("Missing destination in SUBSCRIBE message");
            }

            List<String> tokenHeaders = accessor.getNativeHeader("token");
            List<String> userIdHeaders = accessor.getNativeHeader("userId");
            if(tokenHeaders == null || tokenHeaders.isEmpty()) {
                LOGGER.error("Missing token header");
                throw new AuthorizationServiceException("Missing token header");
            }
            if(userIdHeaders == null || userIdHeaders.isEmpty()) {
                LOGGER.error("Missing userId header");
                throw new AuthorizationServiceException("Missing userId header");
            }
            String token = tokenHeaders.get(0);
            String userId = userIdHeaders.get(0);
            String projectId = extractProjectId(destination);
            LOGGER.info("Validation subscription. User {} project {}", userId, projectId);

            var hasAccessToProject = accessManager.hasPermission(Subject.forUser(userId)
                    , ProjectResource.forProject(ProjectId.valueOf(projectId)), BuiltInCapability.VIEW_PROJECT,
                    new ExecutionContext(UserId.valueOf(userId), token, UUID.randomUUID().toString()));

            if(!hasAccessToProject) {
                throw new AuthorizationServiceException("User " + userId + " does not have access to project " + projectId);
            }

        }


        return message;
    }
    private static String extractProjectId(String input) {
        if(input == null || input.isEmpty()) {
            throw new RuntimeException("Error extracting projectId from destination");
        }

        String[] segments = input.split("/");

        return segments[segments.length - 1];
    }
}
