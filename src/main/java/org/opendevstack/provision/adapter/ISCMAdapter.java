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
     * Create an SCM project / container for various repositories
     * created later thru {@link #createAuxiliaryRepositoriesForODSProject(ProjectData, String, String[])} and
     * {@link #createComponentRepositoriesForODSProject(ProjectData, String)}
     * @param project the project including the project's name and key 
     * {@link ProjectData#key} and {@link ProjectData#name}
     * @param crowdCookie the sso cookie
     * @return the project, filled with {@link ProjectData#bitbucketUrl}
     * @throws IOException in case the project / space cannot be created
     */
    public ProjectData createSCMProjectForODSProject(
            ProjectData project, String crowdCookie)
            throws IOException;

    /**
     * Called to create auxiliary repos, e.g for design artifacts
     * @param project the project including the project's name and key 
     * {@link ProjectData#key} and {@link ProjectData#name}
     * @param crowdCookie the sso cookie
     * @param auxRepos the list of auxiliary repositories
     * @return the project
     * @throws IOException in case something goes wrong during creating
     * these repositories
     */
    public ProjectData createAuxiliaryRepositoriesForODSProject(
            ProjectData project, String crowdCookie,
            String[] auxRepos) throws IOException;

    /**
     * Create repositories based on passed {@link ProjectData#quickstart}
     * @param project the project containing quickstarters - 
     * to derive the names from
     * @param crowdCookie the sso cookie
     * @return the project with filled {@link ProjectData#repositories}
     * @throws IOException in case the repositories cannot be created
     */
    public ProjectData createComponentRepositoriesForODSProject(
            ProjectData project, String crowdCookie)
            throws IOException;

}
