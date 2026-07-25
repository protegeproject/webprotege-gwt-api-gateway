package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.util.concurrent.MoreExecutors;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.websocket.config.ObjectMapperConfiguration;
import edu.stanford.protege.webprotege.gateway.websocket.dto.EventList;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryRequest;
import edu.stanford.protege.webprotege.gateway.websocket.dto.ProjectEventsQueryResponse;
import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryReplaySseCatchUpServiceTest {

    @Mock
    private CommandExecutor<ProjectEventsQueryRequest, ProjectEventsQueryResponse> eventsQueryExecutor;

    private SseStreamRegistry registry;

    private HistoryReplaySseCatchUpService service;

    private ProjectId projectId;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapperConfiguration().objectMapper();
        // Direct dispatch so publish()/completeCatchUp() sends are observable without waiting on threads.
        registry = new SseStreamRegistry(objectMapper,
                                         new SseProperties(),
                                         MoreExecutors.newDirectExecutorService(),
                                         mock(ScheduledExecutorService.class));
        service = new HistoryReplaySseCatchUpService(registry, eventsQueryExecutor);
        projectId = ProjectId.generate();
        executionContext = new ExecutionContext(UserId.valueOf("the-user"), "the-token", "correlation");
    }

    @Test
    void replaysHistorySinceLastEventIdThenFlushesBufferedLiveEvents() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, "4", executionContext);
        // Live events arriving while the history query is in flight are buffered by the registry.
        registry.publish(projectId, 6L, liveResponse());
        registry.publish(projectId, 7L, liveResponse());
        when(eventsQueryExecutor.execute(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(historyResponse(4, 6)));

        service.catchUp(projectId, "4", emitter, executionContext);

        ArgumentCaptor<ProjectEventsQueryRequest> request = ArgumentCaptor.forClass(ProjectEventsQueryRequest.class);
        verify(eventsQueryExecutor).execute(request.capture(), any());
        assertEquals(4, request.getValue().sinceTag.getOrdinal(), "history is queried strictly after the last seen id");
        assertEquals(projectId, request.getValue().projectId);

        ArgumentCaptor<SseEventBuilder> frames = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter, times(2)).send(frames.capture());
        List<String> rendered = frames.getAllValues().stream().map(HistoryReplaySseCatchUpServiceTest::render).toList();
        // Replay first (id = end tag 6), then the one buffered event newer than the replay; buffered 6 dropped.
        assertTrue(rendered.get(0).contains("id:6"), rendered.get(0));
        assertTrue(rendered.get(1).contains("id:7"), rendered.get(1));
    }

    @Test
    void freshConnectionWithoutLastEventIdIsLiveOnlyAndQueriesNoHistory() {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, null, executionContext);

        service.catchUp(projectId, null, emitter, executionContext);

        verify(eventsQueryExecutor, never()).execute(any(), any());
    }

    @Test
    void failedHistoryQueryKeepsStreamLiveInsteadOfKillingIt() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, "4", executionContext);
        registry.publish(projectId, 5L, liveResponse());
        when(eventsQueryExecutor.execute(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("history unavailable")));

        service.catchUp(projectId, "4", emitter, executionContext);
        // Stream survived and resumed live: a later event is delivered straight through.
        registry.publish(projectId, 6L, liveResponse());

        verify(emitter, never()).completeWithError(any());
        ArgumentCaptor<SseEventBuilder> frames = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter, times(2)).send(frames.capture());
        List<String> rendered = frames.getAllValues().stream().map(HistoryReplaySseCatchUpServiceTest::render).toList();
        // No replay frame; the buffered 5 flushes and then live 6 flows through.
        assertTrue(rendered.get(0).contains("id:5"), rendered.get(0));
        assertTrue(rendered.get(1).contains("id:6"), rendered.get(1));
    }

    private static ProjectEventsQueryResponse liveResponse() {
        ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
        response.events = new EventList(EventTag.getFirst(), JsonNodeFactory.instance.arrayNode(), EventTag.get(1));
        return response;
    }

    private static ProjectEventsQueryResponse historyResponse(int startTag, int endTag) {
        ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
        response.events = new EventList(EventTag.get(startTag), JsonNodeFactory.instance.arrayNode(), EventTag.get(endTag));
        return response;
    }

    private static String render(SseEventBuilder builder) {
        Set<DataWithMediaType> parts = builder.build();
        return parts.stream().map(part -> String.valueOf(part.getData())).collect(Collectors.joining());
    }
}
