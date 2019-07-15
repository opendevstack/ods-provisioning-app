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

package org.opendevstack.provision.storage;

import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.OpenProjectData;

import java.io.IOException;
import java.util.Map;
/**
 * @author Torsten Jaeschke Interface for the underlying storage
 */
public interface IStorage {
    /**
   * Store a project
   * 
     * @param project the project to store
     * @return the filepath the file is stored at
     */
    String storeProject(OpenProjectData project) throws IOException;

    /**
     * Get the project history, ordered by date
   * 
     * @return the project history as map
     */
    Map<String, OpenProjectData> listProjectHistory();

    /**
     * get a project by its key
   * 
     * @param key the project's key
     * @return the project by its key, or null in case not found
     */
    OpenProjectData getProject(String key);

    /**
     * Update an already existing project
   * 
   * @param project the project to update (based on the {@link OpenProjectData#projectKey})
     * @return the filepath of the stored project
     * @throws IOException in case the project cannot be stored
     */
  boolean updateStoredProject(OpenProjectData project) throws IOException;

    /**
     * Store the about changes data
   * 
     * @param aboutData the data
     * @return the filepath
     * @throws IOException in case the data cannot be stored
     */
  String storeAboutChangesData(AboutChangesData aboutData) throws IOException;

    /**
     * Get the about history
   * 
     * @return the changes
     */
    AboutChangesData listAboutChangesData();

    /**
     * Return the storage path
   * 
     * @return
     */
    String getStoragePath();

  boolean deleteProject(OpenProjectData project);
}
