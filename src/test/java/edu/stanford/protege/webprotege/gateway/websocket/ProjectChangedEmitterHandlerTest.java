package edu.stanford.protege.webprotege.gateway.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.websocket.config.ObjectMapperConfiguration;
import edu.stanford.protege.webprotege.gateway.websocket.dto.EventList;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.gateway.websocket.dto.SequencedPackagedProjectChangeEvent;
import edu.stanford.protege.webprotege.tag.EntityTagsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.IRI;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectChangedEmitterHandlerTest {

    private static final int SEQUENCE_NUMBER = 5;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    private ProjectChangedEmitterHandler eventHandler;

    private SequencedPackagedProjectChangeEvent sequencedEvent;

    private EntityTagsChangedEvent entityTagsChangedEvent;
    private ProjectId projectId;

    private EventId eventId;

    private ObjectMapper objectMapper;

    private final ArgumentCaptor<GenericMessage> websocketCaptor = ArgumentCaptor.forClass(GenericMessage.class);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperConfiguration().objectMapper();
        eventHandler = new ProjectChangedEmitterHandler(simpMessagingTemplate, objectMapper);
        projectId = ProjectId.generate();
        eventId = EventId.generate();
        entityTagsChangedEvent = new EntityTagsChangedEvent(new EventId("eventId"),
                projectId,
                new OWLClassImpl(IRI.create("http://www.example.org/R9UuCy8Vzvft2f4fc67VwGs")),
                new ArrayList<>());
        sequencedEvent = new SequencedPackagedProjectChangeEvent(projectId, eventId, SEQUENCE_NUMBER, List.of(entityTagsChangedEvent));
    }

    @Test
    void GIVEN_handler_WHEN_registered_THEN_listensOnSequencedChannel() {
        assertEquals(SequencedPackagedProjectChangeEvent.CHANNEL, eventHandler.getChannelName());
        assertEquals("webprotege.events.projects.SequencedPackagedProjectChange", eventHandler.getChannelName());
        assertEquals(SequencedPackagedProjectChangeEvent.class, eventHandler.getEventClass());
    }

    @Test
    void GIVEN_sequencedEvent_WHEN_handleEvent_THEN_eventIsPushedToWebsocket() throws JsonProcessingException {
        eventHandler.handleEvent(sequencedEvent);

        verify(simpMessagingTemplate).send(eq("/topic/project-events/" + projectId.id()), websocketCaptor.capture());

        var capturedMessage = websocketCaptor.getValue();
        ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
        response.events = new EventList(EventTag.get(SEQUENCE_NUMBER - 1), sequencedEvent.projectEvents(), EventTag.get(SEQUENCE_NUMBER));

        String expectedEvent = objectMapper.writeValueAsString(response);

        assertEquals(objectMapper.readTree(expectedEvent), objectMapper.readTree(new String((byte[]) capturedMessage.getPayload())));
    }

    @Test
    void GIVEN_sequencedEvent_WHEN_handleEvent_THEN_tagsAreStartSeqMinusOne_endSeq() throws JsonProcessingException {
        eventHandler.handleEvent(sequencedEvent);

        verify(simpMessagingTemplate).send(eq("/topic/project-events/" + projectId.id()), websocketCaptor.capture());

        JsonNode payload = payloadOf(websocketCaptor.getValue());
        JsonNode eventList = payload.get("events");

        assertEquals(SEQUENCE_NUMBER - 1, eventList.get("startTag").asInt());
        assertEquals(SEQUENCE_NUMBER, eventList.get("endTag").asInt());
    }

    /**
     * Locks the STOMP wire payload field names. gwt-ui deserializes this frame as
     * {@code GetProjectEventsResult} with no shared DTO artifact, so the exact JSON shape
     * ({@code @type} + {@code events{startTag, events, endTag}}) is the contract.
     */
    @Test
    void GIVEN_sequencedEvent_WHEN_handleEvent_THEN_payloadHasExpectedJsonShape() throws JsonProcessingException {
        eventHandler.handleEvent(sequencedEvent);

        verify(simpMessagingTemplate).send(eq("/topic/project-events/" + projectId.id()), websocketCaptor.capture());

        JsonNode payload = payloadOf(websocketCaptor.getValue());

        assertEquals("webprotege.hierarchies.GetProjectEvents", payload.get("@type").asText());
        assertTrue(payload.has("events"), "payload must carry an 'events' object");

        JsonNode eventList = payload.get("events");
        assertTrue(eventList.has("startTag"), "event list must carry 'startTag'");
        assertTrue(eventList.has("events"), "event list must carry 'events'");
        assertTrue(eventList.has("endTag"), "event list must carry 'endTag'");
    }

    private JsonNode payloadOf(GenericMessage<?> message) throws JsonProcessingException {
        return objectMapper.readTree(new String((byte[]) message.getPayload()));
    }
}
