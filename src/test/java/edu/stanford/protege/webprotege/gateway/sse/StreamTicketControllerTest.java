package edu.stanford.protege.webprotege.gateway.sse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StreamTicketControllerTest {

    @Mock
    private StreamTicketService ticketService;

    private MockMvc mockMvc;

    private ProjectId projectId;

    @BeforeEach
    void setUp() {
        // Mirror the production ObjectMapper's lenient unknown-property handling so a body that smuggles an
        // extra field is accepted (and ignored) rather than rejected outright.
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        StreamTicketController controller = new StreamTicketController(ticketService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        projectId = ProjectId.generate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTicketWhenAuthorized() throws Exception {
        authenticateAs("alice", "alice-token");
        when(ticketService.issueTicket(eq(new UserId("alice")), eq(projectId), eq("alice-token")))
                .thenReturn(Optional.of(new StreamTicketService.IssuedTicket("the-ticket", 120)));

        mockMvc.perform(post(StreamTicketController.TICKET_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(projectId.id())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ticket").value("the-ticket"))
               .andExpect(jsonPath("$.expiresIn").value(120));
    }

    @Test
    void returnsForbiddenWhenServiceDenies() throws Exception {
        authenticateAs("alice", "alice-token");
        when(ticketService.issueTicket(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post(StreamTicketController.TICKET_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(projectId.id())))
               .andExpect(status().isForbidden());
    }

    @Test
    void identityIsTakenFromTheTokenNotTheBody() throws Exception {
        authenticateAs("alice", "alice-token");
        when(ticketService.issueTicket(any(), any(), any()))
                .thenReturn(Optional.of(new StreamTicketService.IssuedTicket("the-ticket", 120)));

        mockMvc.perform(post(StreamTicketController.TICKET_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"projectId\":\"" + projectId.id() + "\",\"userId\":\"attacker\"}"))
               .andExpect(status().isOk());

        ArgumentCaptor<UserId> userId = ArgumentCaptor.forClass(UserId.class);
        verify(ticketService).issueTicket(userId.capture(), eq(projectId), eq("alice-token"));
        assertThat(userId.getValue()).isEqualTo(new UserId("alice"));
    }

    @Test
    void rejectsMissingProjectId() throws Exception {
        authenticateAs("alice", "alice-token");

        mockMvc.perform(post(StreamTicketController.TICKET_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
               .andExpect(status().isBadRequest());

        verify(ticketService, never()).issueTicket(any(), any(), any());
    }

    private static String body(String projectId) {
        return "{\"projectId\":\"" + projectId + "\"}";
    }

    private static void authenticateAs(String username, String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .claim("preferred_username", username)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
