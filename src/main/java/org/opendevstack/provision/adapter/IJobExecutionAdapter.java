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

public interface IJobExecutionAdapter extends IServiceAdapter
{

    public List<Job> getQuickstarters();

    public ProjectData createOpenshiftProjects(ProjectData project,
            String crowdCookie) throws IOException;

    public List<ExecutionsData> provisionComponentsBasedOnQuickstarters(
            ProjectData project) throws IOException;

}
