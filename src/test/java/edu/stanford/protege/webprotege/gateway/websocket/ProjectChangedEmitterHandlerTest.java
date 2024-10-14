package edu.stanford.protege.webprotege.gateway.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.websocket.config.ObjectMapperConfiguration;
import edu.stanford.protege.webprotege.gateway.websocket.dto.EventList;
import edu.stanford.protege.webprotege.gateway.websocket.dto.PackagedProjectChangeEvent;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.tag.EntityTagsChangedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.util.ArrayList;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProjectChangedEmitterHandlerTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    private ProjectChangedEmitterHandler eventHandler;

    private PackagedProjectChangeEvent packagedProjectChangeEvent;

    private EntityTagsChangedEvent entityTagsChangedEvent;
    private ProjectId projectId;

    private EventId eventId;

    private ObjectMapper objectMapper;

    private final ArgumentCaptor<GenericMessage> websocketCaptor = ArgumentCaptor.forClass(GenericMessage.class);

    @Before
    public void setUp() {
        objectMapper = new ObjectMapperConfiguration().objectMapper();
        eventHandler = new ProjectChangedEmitterHandler(simpMessagingTemplate, objectMapper);
        projectId = ProjectId.generate();
        eventId = EventId.generate();
        entityTagsChangedEvent = new EntityTagsChangedEvent(new EventId("eventId"),
                projectId,
                new OWLClassImpl(IRI.create("http://www.example.org/R9UuCy8Vzvft2f4fc67VwGs")),
                new ArrayList<>());
        packagedProjectChangeEvent = new PackagedProjectChangeEvent(projectId, eventId, List.of(entityTagsChangedEvent));
    }

    //
    @Test
    public void GIVEN_entityTagsChangedEvent_WHEN_registerEvent_THEN_eventIsPushedToWebsocket() throws JsonProcessingException {
        eventHandler.handleEvent(packagedProjectChangeEvent);


        verify(simpMessagingTemplate).send(eq("/topic/project-events/" + projectId.id()), websocketCaptor.capture());

        var capturedMessage = websocketCaptor.getValue();
        ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
        response.events = new EventList(EventTag.getFirst(), packagedProjectChangeEvent.projectEvents(), EventTag.get(1));

        String expectedEvent = objectMapper.writeValueAsString(response);


        assertEquals(objectMapper.readTree(expectedEvent), objectMapper.readTree(new String( (byte[]) capturedMessage.getPayload())));

    }

}
