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
import java.util.List;

import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.rundeck.Job;

/**
 * Job execution adapter, called to delegate long running
 * provision, e.g. of components and boilerplates
 * @author utschig
 *
 */
public interface IJobExecutionAdapter extends IServiceAdapter
{

    /**
     * Return the list of available quickstarters
     * @return the list of quickstarters, never null but can be empty
     */
    public List<Job> getQuickstarters();

    /**
     * Create platform projects, e.g. openshift projects to house later
     * components created thru {@link #provisionComponentsBasedOnQuickstarters(ProjectData)}
     * @param project the project including the project's name and key 
     * {@link ProjectData#key} and {@link ProjectData#name}
     * @param crowdCookie the sso cookie
     * @return the project with filled
     * {@link ProjectData#openshiftConsoleDevEnvUrl}, 
     * {@link ProjectData#openshiftConsoleTestEnvUrl},
     * {{@link ProjectData#openshiftJenkinsUrl} and
     * {@link ProjectData#lastJobs} which contains the link the jobs
     * that were kicked off
     * @throws IOException in case the projects cannot be created
     */
    public ProjectData createPlatformProjects(ProjectData project,
            String crowdCookie) throws IOException;

    /**
     * Create repositories based on passed {@link ProjectData#quickstart}
     * @param project the project containing quickstarters - 
     * to derive the names from
     * @param crowdCookie the sso cookie
     * @return the project filled with {@link ProjectData#lastJobs} 
     * which contains the URLs to the jobs kicked off.
     * @throws IOException
     */
    public List<ExecutionsData> provisionComponentsBasedOnQuickstarters(
            ProjectData project) throws IOException;

}
