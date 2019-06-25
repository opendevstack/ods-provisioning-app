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

import org.opendevstack.provision.model.ProjectData;

public interface IBugtrackerAdapter extends IServiceAdapter
{

    public ProjectData createBugtrackerProjectForODSProject(
            ProjectData project, String crowdCookieValue)
            throws IOException;

    public int addShortcutsToProject(ProjectData data,
            String crowdCookieValue);

    public boolean keyExists(String key, String crowdCookieValue);

    public String buildProjectKey(String name);

    public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
            ProjectData project);

}
