package org.opentripplanner.middleware.utils.bugsnag;

import org.opentripplanner.middleware.utils.bugsnag.response.Organization;
import org.opentripplanner.middleware.utils.bugsnag.response.Project;
import org.opentripplanner.middleware.utils.bugsnag.response.ProjectError;

import java.util.List;

public interface BugsnagDispatcher {

    List<Organization> getOrganization();
    List<Project> getProjects(String organizationId);
    List<ProjectError> getAllProjectErrors(String projectId);
}
