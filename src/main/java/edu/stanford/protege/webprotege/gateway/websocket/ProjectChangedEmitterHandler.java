package edu.stanford.protege.webprotege.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.sse.SseStreamRegistry;
import edu.stanford.protege.webprotege.gateway.websocket.dto.EventList;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.gateway.websocket.dto.SequencedPackagedProjectChangeEvent;
import edu.stanford.protege.webprotege.ipc.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ProjectChangedEmitterHandler implements EventHandler<SequencedPackagedProjectChangeEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectChangedEmitterHandler.class);

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final SseStreamRegistry sseStreamRegistry;

    private final ObjectMapper objectMapper;

    public ProjectChangedEmitterHandler(SimpMessagingTemplate simpMessagingTemplate,
                                        SseStreamRegistry sseStreamRegistry,
                                        ObjectMapper objectMapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.sseStreamRegistry = sseStreamRegistry;
        this.objectMapper = objectMapper;
    }


    @Nonnull
    @Override
    public String getChannelName() {
        return SequencedPackagedProjectChangeEvent.CHANNEL;
    }

    @Nonnull
    @Override
    public String getHandlerName() {
        return this.getClass().getName();
    }

    @Override
    public Class<SequencedPackagedProjectChangeEvent> getEventClass() {
        return SequencedPackagedProjectChangeEvent.class;
    }

    @Override
    public void handleEvent(SequencedPackagedProjectChangeEvent event) {
        try {
            int sequenceNumber = event.sequenceNumber();
            ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
            response.events = new EventList(EventTag.get(sequenceNumber - 1), event.projectEvents(), EventTag.get(sequenceNumber));
            // Fan the same payload out to both delivery paths: the SSE streams (id = sequence number)
            // and the legacy STOMP topic. STOMP stays until #308 retires it. publish() serialises on
            // this thread but dispatches each send on its own executor, so a slow client cannot stall
            // the Rabbit listener thread.
            sseStreamRegistry.publish(event.projectId(), sequenceNumber, response);
            simpMessagingTemplate.send("/topic/project-events/" + event.projectId().id(), new GenericMessage<>(objectMapper.writeValueAsBytes(response)));
        } catch (Exception e) {
            // Pass the throwable so the cause/stack are visible in the log.
            LOGGER.error("Error forwarding the events through websocket", e);
        }
    }
}
