package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

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
    private StreamTicketService ticketService;

    private MockMvc mockMvc;

    private ProjectId projectId;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        ProjectEventsSseController controller = new ProjectEventsSseController(registry, catchUpService, ticketService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        projectId = ProjectId.generate();
        executionContext = new ExecutionContext(UserId.valueOf("the-user"), "the-token", UUID.randomUUID().toString());
    }

    @Test
    void returnsUnauthorizedWhenTicketMissing() throws Exception {
        when(ticketService.redeem(isNull(), eq(projectId)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired stream ticket"));

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(status().isUnauthorized());

        verify(registry, never()).subscribe(any(), any(), any());
    }

    @Test
    void returnsUnauthorizedWhenTicketInvalid() throws Exception {
        when(ticketService.redeem(eq("bad-ticket"), eq(projectId)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired stream ticket"));

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("ticket", "bad-ticket")
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(status().isUnauthorized());

        verify(registry, never()).subscribe(any(), any(), any());
    }

    @Test
    void returnsForbiddenWhenTicketIdentityLostAccess() throws Exception {
        when(ticketService.redeem(eq("stale-ticket"), eq(projectId)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no access"));

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("ticket", "stale-ticket")
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(status().isForbidden());

        verify(registry, never()).subscribe(any(), any(), any());
    }

    @Test
    void subscribesWithTheTicketIdentityWhenRedeemed() throws Exception {
        when(ticketService.redeem(eq("good-ticket"), eq(projectId))).thenReturn(executionContext);
        when(registry.subscribe(any(), any(), any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("ticket", "good-ticket")
                                .accept(MediaType.TEXT_EVENT_STREAM))
               .andExpect(request().asyncStarted())
               .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
               .andExpect(header().string("X-Accel-Buffering", "no"));

        // Identity flows from the redeemed ticket, not from anything on the request.
        verify(registry).subscribe(eq(projectId), isNull(), eq(executionContext));
        verify(catchUpService).catchUp(eq(projectId), isNull(), any(SseEmitter.class), eq(executionContext));
    }

    @Test
    void prefersLastEventIdHeaderOverQueryParam() throws Exception {
        when(ticketService.redeem(eq("good-ticket"), eq(projectId))).thenReturn(executionContext);
        when(registry.subscribe(any(), any(), any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/data/projects/{projectId}/events", projectId.id())
                                .param("ticket", "good-ticket")
                                .param("lastEventId", "10")
                                .header("Last-Event-ID", "25")
                                .accept(MediaType.TEXT_EVENT_STREAM));

        verify(registry).subscribe(eq(projectId), eq("25"), eq(executionContext));
    }
}
