/*
 * Copyright 2017-2019 the original author or authors.
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

import org.opendevstack.provision.model.OpenProjectData;

/**
 * Interface for SCM adapters
 * 
 * @author utschig
 */
public interface ISCMAdapter extends IServiceAdapter
{
    /**
     * Create an SCM project / container for various repositories
     * created later thru {@link #createAuxiliaryRepositoriesForODSProject(OpenProjectData, String, String[])} and
     * {@link #createComponentRepositoriesForODSProject(OpenProjectData, String)}
     * @param project the project including the project's name and key 
     * {@link OpenProjectData#projectKey} and {@link OpenProjectData#projectName}
     * @return the project, filled with {@link OpenProjectData#scmvcsUrl}
     * @throws IOException in case the project / space cannot be created
     */
    public OpenProjectData createSCMProjectForODSProject(
            OpenProjectData project)
            throws IOException;

    /**
     * Called to create auxiliary repositories, e.g for design artifacts
     * @param project the project including the project's name and key 
     * {@link OpenProjectData#projectKey} and {@link OpenProjectData#projectName}
     * @param auxRepos the list of auxiliary repositories
     * @return the project filled with aux repo information inside 
     * {@link OpenProjectData#repositories}
     * @throws IOException in case something goes wrong during creating
     * these repositories
     */
    public OpenProjectData createAuxiliaryRepositoriesForODSProject(
            OpenProjectData project, String[] auxRepos) throws IOException;

    /**
     * Create repositories based on passed {@link OpenProjectData#quickstarters}
     * @param project the project containing NEW quickstarters ONLY - 
     * to derive the names from
     * @return the project with filled {@link OpenProjectData#repositories}
     * @throws IOException in case the repositories cannot be created
     */
    public OpenProjectData createComponentRepositoriesForODSProject(
            OpenProjectData project) throws IOException;

}
