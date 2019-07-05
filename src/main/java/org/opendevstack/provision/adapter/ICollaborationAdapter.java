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
 * Interface for collaboration adapter implementations, e.g for confluence
 * @author utschig
 *
 */
public interface ICollaborationAdapter extends IServiceAdapter
{

    /**
     * Called to create a collaboration space
     * @param project the project with {@link ProjectData#name} & 
     * {@link ProjectData#key} filled 
     * @return the project filled with {@link ProjectData#confluenceUrl}
     * @throws IOException in case the space cannot be created
     */
    public OpenProjectData createCollaborationSpaceForODSProject(
            OpenProjectData project)
            throws IOException;

    /**
     * In case templates are used return template(s) based on 
     * {@link ProjectData#projectType}
     * @param project the project with filled projectType
     * @return the template(s) keys
     */
    public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
            OpenProjectData project);

}
