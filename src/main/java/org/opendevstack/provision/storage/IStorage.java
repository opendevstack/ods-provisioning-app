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

package org.opendevstack.provision.storage;

import java.io.IOException;
import java.util.Map;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.ProjectData;

/**
 * @author Torsten Jaeschke
 */

public interface IStorage
{
    String storeProject(ProjectData project) throws IOException;

    Map<String, ProjectData> listProjectHistory();

    ProjectData getProject(String id);

    boolean updateStoredProject(ProjectData project)
            throws IOException;

    String storeAboutChangesData(AboutChangesData aboutData)
            throws IOException;

    AboutChangesData listAboutChangesData();
}
