package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.impl.CommandExecutorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the command executor the SSE catch-up path uses to pull missed events from the durable
 * event-history service on {@code webprotege.hierarchies.GetProjectEvents}. Declared the same way the
 * rest of the platform declares {@link CommandExecutor} beans (see the ipc application's
 * authorization-status executor).
 */
@Configuration
public class SseCommandExecutorConfiguration {

    @Bean
    CommandExecutor<ProjectEventsQueryRequest, ProjectEventsQueryResponse> projectEventsQueryExecutor() {
        return new CommandExecutorImpl<>(ProjectEventsQueryResponse.class);
    }
}
