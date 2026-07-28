package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.gateway.ObjectMapperConfiguration;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.MoreExecutors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SseStreamRegistryTest {

    private SseStreamRegistry registry;

    private ProjectId projectId;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapperConfiguration().objectMapper();
        SseProperties properties = new SseProperties();
        // Run fan-out synchronously so sends are observable without waiting on threads.
        ExecutorService dispatchExecutor = MoreExecutors.newDirectExecutorService();
        ScheduledExecutorService heartbeatExecutor = mock(ScheduledExecutorService.class);
        registry = new SseStreamRegistry(objectMapper, properties, dispatchExecutor, heartbeatExecutor);
        projectId = ProjectId.generate();
        executionContext = new ExecutionContext(UserId.valueOf("the-user"), "the-token", "correlation");
    }

    @Test
    void subscribeRegistersSubscriberAndStoresLastEventId() {
        registry.subscribe(projectId, "42", executionContext);

        assertEquals(1, registry.subscriberCount(projectId));
        assertEquals("42", registry.subscribersFor(projectId).get(0).lastEventId());
    }

    @Test
    void publishSendsSequenceStampedEventToSubscriber() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, null, executionContext);

        registry.publish(projectId, 7L, response());

        ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String frame = render(captor.getValue());
        assertTrue(frame.contains("id:7"), frame);
        assertTrue(frame.contains("event:project-events"), frame);
        assertTrue(frame.contains("webprotege.hierarchies.GetProjectEvents"), frame);
    }

    @Test
    void publishEvictsSubscriberWhenSendFails() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEventBuilder.class));
        registry.register(projectId, emitter, null, executionContext);

        registry.publish(projectId, 1L, response());

        assertEquals(0, registry.subscriberCount(projectId));
    }

    @Test
    void heartbeatSendsKeepAliveComment() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, null, executionContext);

        registry.heartbeat();

        ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        assertTrue(render(captor.getValue()).contains("event:heartbeat"));
    }

    @Test
    void heartbeatEvictsSubscriberWhenSendFails() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEventBuilder.class));
        registry.register(projectId, emitter, null, executionContext);

        registry.heartbeat();

        assertEquals(0, registry.subscriberCount(projectId));
    }

    @Test
    void completionCallbackEvictsSubscriber() {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, null, executionContext);
        ArgumentCaptor<Runnable> onCompletion = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(onCompletion.capture());
        assertEquals(1, registry.subscriberCount(projectId));

        onCompletion.getValue().run();

        assertEquals(0, registry.subscriberCount(projectId));
    }

    @Test
    void subscriberWithLastEventIdBuffersLiveEventsUntilCatchUp() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, "3", executionContext);

        registry.publish(projectId, 4L, response());

        // Held back until the reconnect's history replay flushes, so it cannot overtake the replay.
        verify(emitter, never()).send(any(SseEventBuilder.class));
        assertEquals(1, registry.subscriberCount(projectId));
    }

    @Test
    void completeCatchUpEmitsReplayThenFlushesBufferDroppingDuplicates() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, "4", executionContext);
        registry.publish(projectId, 5L, response());
        registry.publish(projectId, 6L, response());
        registry.publish(projectId, 7L, response());

        // Replay covers through seq 6; buffered 5 and 6 are duplicates, only 7 survives.
        registry.completeCatchUp(projectId, emitter, 6L, response());

        ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter, times(2)).send(captor.capture());
        List<String> frames = captor.getAllValues().stream().map(SseStreamRegistryTest::render).toList();
        assertTrue(frames.get(0).contains("id:6"), frames.get(0));
        assertTrue(frames.get(1).contains("id:7"), frames.get(1));
    }

    @Test
    void completeCatchUpWithoutReplayFlushesBufferedThenResumesLiveDelivery() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(projectId, emitter, "4", executionContext);
        registry.publish(projectId, 5L, response());

        // No replay (e.g. history query failed); buffered 5 (> 4) still flushes, stream stays open.
        registry.completeCatchUp(projectId, emitter, 4L, null);
        // Live delivery has resumed: a later event is sent straight through, not buffered.
        registry.publish(projectId, 6L, response());

        ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
        verify(emitter, times(2)).send(captor.capture());
        List<String> frames = captor.getAllValues().stream().map(SseStreamRegistryTest::render).toList();
        assertTrue(frames.get(0).contains("id:5"), frames.get(0));
        assertTrue(frames.get(1).contains("id:6"), frames.get(1));
    }

    private static ProjectEventsQueryResponse response() {
        ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
        response.events = new EventList(EventTag.getFirst(), JsonNodeFactory.instance.arrayNode(), EventTag.get(1));
        return response;
    }

    private static String render(SseEventBuilder builder) {
        Set<DataWithMediaType> parts = builder.build();
        return parts.stream().map(part -> String.valueOf(part.getData())).collect(Collectors.joining());
    }
}
