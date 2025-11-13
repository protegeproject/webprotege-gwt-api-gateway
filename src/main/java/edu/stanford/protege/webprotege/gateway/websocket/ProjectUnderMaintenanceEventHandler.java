package edu.stanford.protege.webprotege.gateway.websocket;

import edu.stanford.protege.webprotege.gateway.websocket.config.events.ProjectUnderMaintenanceUpdateEvent;
import edu.stanford.protege.webprotege.ipc.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ProjectUnderMaintenanceEventHandler implements EventHandler<ProjectUnderMaintenanceUpdateEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectUnderMaintenanceEventHandler.class);

    private final ProjectMaintenanceState maintenanceState;

    public ProjectUnderMaintenanceEventHandler(ProjectMaintenanceState maintenanceState) {
        this.maintenanceState = maintenanceState;
    }

    @Nonnull
    @Override
    public String getChannelName() {
        return ProjectUnderMaintenanceUpdateEvent.CHANNEL;
    }

    @Nonnull
    @Override
    public String getHandlerName() {
        return this.getClass().getName();
    }

    @Override
    public Class<ProjectUnderMaintenanceUpdateEvent> getEventClass() {
        return ProjectUnderMaintenanceUpdateEvent.class;
    }

    @Override
    public void handleEvent(ProjectUnderMaintenanceUpdateEvent event) {
        try {
            maintenanceState.setUnderMaintenance(event.projectId(), event.underMaintenance());
            LOGGER.info("Project {} maintenance state updated to: {}", event.projectId().id(), event.underMaintenance());
        } catch (Exception e) {
            LOGGER.error("Error handling ProjectUnderMaintenanceUpdateEvent for project {}", event.projectId().id(), e);
        }
    }
}

