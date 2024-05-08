package edu.stanford.protege.webprotege.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.websocket.dto.EventList;
import edu.stanford.protege.webprotege.gateway.websocket.dto.PackagedProjectChangeEvent;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.ipc.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ProjectChangedEmitterHandler implements EventHandler<PackagedProjectChangeEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectChangedEmitterHandler.class);

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ObjectMapper objectMapper;

    public ProjectChangedEmitterHandler(SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
    }


    @Nonnull
    @Override
    public String getChannelName() {
        return "webprotege.events.projects.PackagedProjectChange";
    }

    @Nonnull
    @Override
    public String getHandlerName() {
        return this.getClass().getName();
    }

    @Override
    public Class<PackagedProjectChangeEvent> getEventClass() {
        return PackagedProjectChangeEvent.class;
    }

    @Override
    public void handleEvent(PackagedProjectChangeEvent event) {
        try {
            ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
            response.events = new EventList(EventTag.getFirst(), event.projectEvents(), EventTag.get(1));
            simpMessagingTemplate.send("/topic/project-events/" + event.projectId().id(), new GenericMessage<>(objectMapper.writeValueAsBytes(response)));

        } catch (Exception e) {
            LOGGER.error("Error forwarding the events through websocket");
        }
    }
}
