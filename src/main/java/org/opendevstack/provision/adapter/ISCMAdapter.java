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
import org.opendevstack.provision.model.ProjectData;

/**
 * Interface for SCM adapters
 * 
 * @author utschig
 */
public interface ISCMAdapter extends IServiceAdapter
{

    /**
     * Create a project space in the target SCM
     * 
     * @param projectKey
     *            the unique key of the project, never null
     * @param projectName
     *            the unique name of the project, never null
     * @param projectDescription
     *            the description of the project, may be null
     * @param crowdCookieValue
     *            contains the crowd sso cookie as value
     * @param permissions
     *            map with permissions, can be null
     * @return the URI to the newly created project
     * @throws IOException
     *             in case something goes wrong
     */
    public ProjectData createSCMProjectForODSProject(
            ProjectData project, String crowdCookie)
            throws IOException;

    /**
     * Called to create auxiliary repos, oc-config & design
     * 
     * @param projectKey
     *            the project key
     * @param crowdCookieValue
     *            contains the crowd sso cookie as value
     * @param auxiliaryRepos
     *            the list of repo names to be created
     * @return the list of created repositories
     */
    public ProjectData createAuxiliaryRepositoriesForODSProject(
            ProjectData project, String crowdCookie,
            String[] auxRepos) throws IOException;

    /**
     * Called to create quickstarter repositories
     * 
     * @param permissions
     *            map with permissions, can be null
     * @param quickstarterRepos
     *            the list of repo names to be created
     * @param projectKey
     *            the project key
     * @param crowdCookieValue
     *            contains the crowd sso cookie as value
     * @return
     * @throws IOException
     *             in case something goes wrong
     */
    public ProjectData createComponentRepositoriesForODSProject(
            ProjectData project, String crowdCookie)
            throws IOException;

}
