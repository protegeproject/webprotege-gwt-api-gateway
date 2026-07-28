package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.authorization.ProjectResource;
import edu.stanford.protege.webprotege.authorization.Subject;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamTicketServiceTest {

    @Mock
    private StreamTicketStore store;

    @Mock
    private AccessManager accessManager;

    private SseProperties properties;

    private StreamTicketService service;

    private UserId user;

    private ProjectId project;

    @BeforeEach
    void setUp() {
        properties = new SseProperties();
        properties.setTicketTtl(Duration.ofSeconds(90));
        service = new StreamTicketService(store, accessManager, properties);
        user = UserId.valueOf("alice");
        project = ProjectId.generate();
    }

    @Test
    void issuesTicketWhenUserHasViewProject() {
        when(accessManager.hasPermission(eq(Subject.forUser(user)),
                                         eq(ProjectResource.forProject(project)),
                                         eq(BuiltInCapability.VIEW_PROJECT),
                                         any())).thenReturn(true);
        when(store.issue(user, project, "jwt-token")).thenReturn("the-ticket");

        Optional<StreamTicketService.IssuedTicket> issued = service.issueTicket(user, project, "jwt-token");

        assertThat(issued).isPresent();
        assertThat(issued.get().ticket()).isEqualTo("the-ticket");
        assertThat(issued.get().expiresInSeconds()).isEqualTo(90);
    }

    @Test
    void doesNotIssueTicketWhenUserLacksViewProject() {
        when(accessManager.hasPermission(any(), any(), eq(BuiltInCapability.VIEW_PROJECT), any())).thenReturn(false);

        Optional<StreamTicketService.IssuedTicket> issued = service.issueTicket(user, project, "jwt-token");

        assertThat(issued).isEmpty();
        verify(store, never()).issue(any(), any(), any());
    }

    @Test
    void issuanceChecksPermissionAsTheCallersOwnIdentity() {
        when(accessManager.hasPermission(any(), any(), any(), any())).thenReturn(true);
        when(store.issue(any(), any(), any())).thenReturn("the-ticket");

        service.issueTicket(user, project, "jwt-token");

        ArgumentCaptor<ExecutionContext> context = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(accessManager).hasPermission(eq(Subject.forUser(user)), any(), any(), context.capture());
        assertThat(context.getValue().userId()).isEqualTo(user);
        assertThat(context.getValue().jwt()).isEqualTo("jwt-token");
    }

    @Test
    void redeemReturnsContextFromRecordWhenStillAuthorized() {
        when(store.redeem("the-ticket")).thenReturn(Optional.of(recordFor(project)));
        when(accessManager.hasPermission(eq(Subject.forUser(user)),
                                         eq(ProjectResource.forProject(project)),
                                         eq(BuiltInCapability.VIEW_PROJECT),
                                         any())).thenReturn(true);

        ExecutionContext context = service.redeem("the-ticket", project);

        assertThat(context.userId()).isEqualTo(user);
        assertThat(context.jwt()).isEqualTo("jwt-token");
    }

    @Test
    void redeemMissingTicketIsUnauthorized() {
        assertUnauthorized(() -> service.redeem(null, project));
        assertUnauthorized(() -> service.redeem("  ", project));
        verifyNoInteractions(store);
    }

    @Test
    void redeemUnknownOrExpiredTicketIsUnauthorized() {
        when(store.redeem("bogus")).thenReturn(Optional.empty());

        assertUnauthorized(() -> service.redeem("bogus", project));
        verify(accessManager, never()).hasPermission(any(), any(), any(), any());
    }

    @Test
    void redeemTicketBoundToAnotherProjectIsUnauthorized() {
        ProjectId otherProject = ProjectId.generate();
        when(store.redeem("the-ticket")).thenReturn(Optional.of(recordFor(otherProject)));

        assertUnauthorized(() -> service.redeem("the-ticket", project));
        // Permission is never even consulted: a project-scoped ticket cannot open a different project's stream.
        verify(accessManager, never()).hasPermission(any(), any(), any(), any());
    }

    @Test
    void redeemIsForbiddenWhenIdentityHasLostViewProject() {
        when(store.redeem("stale-ticket")).thenReturn(Optional.of(recordFor(project)));
        when(accessManager.hasPermission(any(), any(), eq(BuiltInCapability.VIEW_PROJECT), any())).thenReturn(false);

        assertThatThrownBy(() -> service.redeem("stale-ticket", project))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void ticketCanBeRedeemedRepeatedlyWhileValid() {
        when(store.redeem("the-ticket")).thenReturn(Optional.of(recordFor(project)));
        when(accessManager.hasPermission(any(), any(), any(), any())).thenReturn(true);

        service.redeem("the-ticket", project);
        ExecutionContext second = service.redeem("the-ticket", project);

        assertThat(second.userId()).isEqualTo(user);
        verify(store, times(2)).redeem("the-ticket");
    }

    private StreamTicket recordFor(ProjectId projectId) {
        return new StreamTicket(user, projectId, "jwt-token", Instant.now().plusSeconds(90));
    }

    private void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
