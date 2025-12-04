package edu.stanford.protege.webprotege.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when a project is under maintenance and requests cannot be processed.
 */
public class ProjectUnderMaintenanceException extends ResponseStatusException {

    public ProjectUnderMaintenanceException(String projectId) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Project " + projectId + " is currently under maintenance");
    }
}

