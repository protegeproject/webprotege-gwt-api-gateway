package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.authorization.ProjectResource;
import edu.stanford.protege.webprotege.authorization.Subject;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.gateway.websocket.AccessManager;
import edu.stanford.protege.webprotege.gateway.websocket.dto.BuiltInCapability;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectEventsSseControllerTest {

    @Mock
    private SseStreamRegistry registry;

    @Mock
    private SseCatchUpService catchUpService;

    @Mock
    private AccessManager accessManager;

    private MockMvc mockMvc;

    private ProjectId projectId;

    @BeforeEach
    void setUp() {
        ProjectEventsSseController controller = new ProjectEventsSseController(registry, catchUpService, accessManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        projectId = ProjectId.generate();
    }

    @Test
    void returnsForbiddenWhenAccessManagerDenies() throws Exception {
        when(accessManager.hasPermission(any(), any(), eq(BuiltInCapability.VIEW_PROJECT), any())).thenReturn(false);

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("userId", "the-user")
                                .param("token", "the-token")
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(status().isForbidden());

        verify(registry, never()).subscribe(any(), any(), any());
    }

    @Test
    void setsSseHeadersAndSubscribesWhenAuthorized() throws Exception {
        when(accessManager.hasPermission(any(), any(), eq(BuiltInCapability.VIEW_PROJECT), any())).thenReturn(true);
        when(registry.subscribe(any(), any(), any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("userId", "the-user")
                                .param("token", "the-token")
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(request().asyncStarted())
               .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
               .andExpect(header().string("X-Accel-Buffering", "no"));

        verify(registry).subscribe(eq(projectId), isNull(), any(ExecutionContext.class));
        verify(accessManager).hasPermission(eq(Subject.forUser("the-user")),
                                            eq(ProjectResource.forProject(projectId)),
                                            eq(BuiltInCapability.VIEW_PROJECT),
                                            any());
    }

    @Test
    void prefersLastEventIdHeaderOverQueryParam() throws Exception {
        when(accessManager.hasPermission(any(), any(), any(), any())).thenReturn(true);
        when(registry.subscribe(any(), any(), any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("userId", "the-user")
                                .param("token", "the-token")
                                .param("lastEventId", "10")
                                .header("Last-Event-ID", "25")
                                .accept(MediaType.TEXT_EVENT_STREAM));

        verify(registry).subscribe(eq(projectId), eq("25"), any());
    }
}
