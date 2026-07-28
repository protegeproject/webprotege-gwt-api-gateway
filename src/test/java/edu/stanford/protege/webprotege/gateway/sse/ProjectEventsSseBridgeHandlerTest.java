package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.gateway.ObjectMapperConfiguration;
import edu.stanford.protege.webprotege.tag.EntityTagsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectEventsSseBridgeHandlerTest {

    private static final int SEQUENCE_NUMBER = 5;

    @Mock
    private SseStreamRegistry sseStreamRegistry;

    private ProjectEventsSseBridgeHandler eventHandler;

    private SequencedPackagedProjectChangeEvent sequencedEvent;

    private EntityTagsChangedEvent entityTagsChangedEvent;

    private ProjectId projectId;

    private EventId eventId;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperConfiguration().objectMapper();
        eventHandler = new ProjectEventsSseBridgeHandler(sseStreamRegistry);
        projectId = ProjectId.generate();
        eventId = EventId.generate();
        entityTagsChangedEvent = new EntityTagsChangedEvent(new EventId("eventId"),
                projectId,
                new OWLClassImpl(IRI.create("http://www.example.org/R9UuCy8Vzvft2f4fc67VwGs")),
                new ArrayList<>());
        sequencedEvent = new SequencedPackagedProjectChangeEvent(projectId, eventId, SEQUENCE_NUMBER,
                                                                  objectMapper.valueToTree(List.of(entityTagsChangedEvent)));
    }

    @Test
    void GIVEN_handler_WHEN_registered_THEN_listensOnSequencedChannel() {
        assertEquals(SequencedPackagedProjectChangeEvent.CHANNEL, eventHandler.getChannelName());
        assertEquals("webprotege.events.projects.SequencedPackagedProjectChange", eventHandler.getChannelName());
        assertEquals(SequencedPackagedProjectChangeEvent.class, eventHandler.getEventClass());
    }

    @Test
    void GIVEN_sequencedEvent_WHEN_handleEvent_THEN_publishedToSseRegistryWithSequenceIdAndTags() {
        eventHandler.handleEvent(sequencedEvent);

        ArgumentCaptor<ProjectEventsQueryResponse> sseCaptor = ArgumentCaptor.forClass(ProjectEventsQueryResponse.class);
        verify(sseStreamRegistry).publish(eq(projectId), eq((long) SEQUENCE_NUMBER), sseCaptor.capture());

        EventList events = sseCaptor.getValue().events;
        assertEquals(SEQUENCE_NUMBER - 1, events.startTag().getOrdinal());
        assertEquals(SEQUENCE_NUMBER, events.endTag().getOrdinal());
        assertEquals(sequencedEvent.projectEvents(), events.events());
    }

    /**
     * Locks the SSE wire payload field names. gwt-ui deserializes each frame as
     * {@code GetProjectEventsResult} with no shared DTO artifact, so the exact JSON shape
     * ({@code @type} + {@code events{startTag, events, endTag}}) is the contract.
     */
    @Test
    void GIVEN_sequencedEvent_WHEN_handleEvent_THEN_payloadHasExpectedJsonShape() throws JsonProcessingException {
        eventHandler.handleEvent(sequencedEvent);

        ArgumentCaptor<ProjectEventsQueryResponse> sseCaptor = ArgumentCaptor.forClass(ProjectEventsQueryResponse.class);
        verify(sseStreamRegistry).publish(eq(projectId), eq((long) SEQUENCE_NUMBER), sseCaptor.capture());

        JsonNode payload = objectMapper.readTree(objectMapper.writeValueAsString(sseCaptor.getValue()));

        assertEquals("webprotege.hierarchies.GetProjectEvents", payload.get("@type").asText());
        assertTrue(payload.has("events"), "payload must carry an 'events' object");

        JsonNode eventList = payload.get("events");
        assertTrue(eventList.has("startTag"), "event list must carry 'startTag'");
        assertTrue(eventList.has("events"), "event list must carry 'events'");
        assertTrue(eventList.has("endTag"), "event list must carry 'endTag'");
        assertEquals(SEQUENCE_NUMBER - 1, eventList.get("startTag").asInt());
        assertEquals(SEQUENCE_NUMBER, eventList.get("endTag").asInt());
    }
}
