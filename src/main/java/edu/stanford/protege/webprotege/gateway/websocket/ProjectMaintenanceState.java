package edu.stanford.protege.webprotege.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.impl.CommandExecutorImpl;
import edu.stanford.protege.webprotege.projects.GetProjectDetailsAction;
import edu.stanford.protege.webprotege.projects.GetProjectDetailsResult;
import edu.stanford.protege.webprotege.projects.ProjectDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

@Component
public class ProjectMaintenanceState {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectMaintenanceState.class);

    private final ConcurrentMap<ProjectId, Boolean> maintenanceState = new ConcurrentHashMap<>();
    private final CommandExecutorImpl<GetProjectDetailsAction, GetProjectDetailsResult> getProjectDetailsExecutor;

    public ProjectMaintenanceState(ObjectMapper objectMapper, AsyncRabbitTemplate asyncRabbitTemplate) {
        this.getProjectDetailsExecutor = new CommandExecutorImpl<>(GetProjectDetailsResult.class);
        this.getProjectDetailsExecutor.setAsyncRabbitTemplate(asyncRabbitTemplate);
        this.getProjectDetailsExecutor.setObjectMapper(objectMapper);
    }

    public void setUnderMaintenance(ProjectId projectId, boolean underMaintenance) {
        maintenanceState.put(projectId, underMaintenance);
    }

    public boolean isUnderMaintenance(ProjectId projectId, ExecutionContext executionContext) {
        Boolean cachedValue = maintenanceState.get(projectId);
        if (cachedValue != null) {
            return cachedValue;
        }

        try {
            GetProjectDetailsResult result = getProjectDetailsExecutor.execute(
                    new GetProjectDetailsAction(projectId),
                    executionContext
            ).get();

            ProjectDetails projectDetails = result.projectDetails();
            boolean underMaintenance = projectDetails.underMaintenance();
            
            maintenanceState.put(projectId, underMaintenance);
            
            LOGGER.info("Fetched maintenance state for project {} from backend: {}", projectId.id(), underMaintenance);
            return underMaintenance;

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error fetching project details for project {} from backend", projectId.id(), e);
            // În caz de eroare, returnăm false (nu este sub maintenance)
            return false;
        }
    }
    public void removeProject(ProjectId projectId) {
        maintenanceState.remove(projectId);
    }
}

