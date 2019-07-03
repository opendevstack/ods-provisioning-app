/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendevstack.provision.adapter;

import java.io.IOException;
import java.util.Map;

import org.opendevstack.provision.model.OpenProjectData;

/**
 * Service interface for a bugtracker
 * @author utschig
 */
public interface IBugtrackerAdapter extends IServiceAdapter
{
    /**
     * Create a bugtracker project based on name and key
     * @param project the project including the project's name and key 
     * {@link OpenProjectData#projectKey} and {@link OpenProjectData#projectName}
     * @param crowdCookie the sso cookie
     * @return the project filled with {@link ProjectData#jiraUrl}
     * @throws IOException in case something occurs during the outbound 
     * call to the bugtracker
     */
    public OpenProjectData createBugtrackerProjectForODSProject(
            OpenProjectData project, String crowdCookieValue)
            throws IOException;

    /**
     * Add shortcuts / links to other tools used based on the 
     * {@link OpenProjectData} fields, e.g. openshift URLs
     * @param project the project filled with all available information
     * @param crowdCookieValue the SSO cookie
     * @return the number of shortcuts created
     */
    public int addShortcutsToProject(OpenProjectData project,
            String crowdCookieValue);

    /**
     * Verify if a project key & name exists
     * @param projectKeyName the name or key of a given project
     * @param crowdCookieValue the SSO cookie
     * @return true in case it exists, otherwise false
     */
    public boolean projectKeyExists(String projectKeyName, String crowdCookieValue);

    /**
     * Build the project key - e.g. uppercase it, strip special chars
     * @param proposedProjectKey the key to derive the final key from
     * @return the clean key to be used 
     */
    public String buildProjectKey(String proposedProjectKey);

    /**
     * In case templates are used return template(s) based on 
     * {@link OpenProjectData#projectType}
     * @param project the project with filled projectType
     * @return the template(s) keys
     */
    public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
            OpenProjectData project);

    public Map<String, String> createComponentsForProjectRepositories 
        (OpenProjectData data, String crowdCookieValue);
}
