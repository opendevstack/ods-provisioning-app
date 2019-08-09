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
import java.util.Map;
import org.opendevstack.provision.model.OpenProjectData;

/**
 * Interface for SCM adapters
 *
 * @author utschig
 */
public interface ISCMAdapter extends IServiceAdapter {
  /**
   * Type of URLs returned from this adapter
   *
   * @author utschig
   */
  public enum URL_TYPE {
    URL_CLONE_HTTP,
    URL_CLONE_SSH,
    URL_BROWSE_HTTP
  }

  /**
   * Create an SCM project / container for various component repositories created later thru {@link
   * #createAuxiliaryRepositoriesForODSProject(OpenProjectData, String, String[])} and {@link
   * #createComponentRepositoriesForODSProject(OpenProjectData, String)} <b>Special Attention: </b>
   * {@link OpenProjectData#specialPermissionSet} may be true, hence the implementor needs to take
   * care about setting accurate permissions based on {@link OpenProjectData#projectAdminGroup},
   * {@link OpenProjectData#projectAdminUser}, {@link OpenProjectData#projectReadonlyGroup} and
   * {@link OpenProjectData#projectUserGroup}
   *
   * @param project the project including the project's name and key {@link
   *     OpenProjectData#projectKey} and {@link OpenProjectData#projectName}
   * @return the HTTP browse url of the newly created SCM project
   * @throws IOException in case the project / space cannot be created
   */
  public String createSCMProjectForODSProject(OpenProjectData project) throws IOException;

  /**
   * Called to create auxiliary repositories, e.g for design artifacts
   *
   * @param project the project including the project's name and key {@link
   *     OpenProjectData#projectKey} and {@link OpenProjectData#projectName}
   * @param auxRepos the list of auxiliary repositories
   * @return a Map with key being the key for the repo and a map with the repo links
   * @throws IOException in case something goes wrong during creating these repositories
   */
  public Map<String, Map<URL_TYPE, String>> createAuxiliaryRepositoriesForODSProject(
      OpenProjectData project, String[] auxRepos) throws IOException;

  /**
   * Create repositories based on passed {@link OpenProjectData#quickstarters}
   *
   * @param project the project containing NEW quickstarters ONLY - to derive the names from. The
   *     name of the component can be found as option in the map with key being {@link
   *     OpenProjectData#COMPONENT_ID_KEY}
   * @return a Map with key being the key for the repo and a map with the repo links
   * @throws IOException in case the repositories cannot be created
   */
  public Map<String, Map<URL_TYPE, String>> createComponentRepositoriesForODSProject(
      OpenProjectData project) throws IOException;

  /**
   * Create the repository name from the component's name. This is super important, as it's the only
   * way for us to know which repo belongs to which component.
   *
   * @param projectKey the key of the project
   * @param componentName the component's name
   * @return
   */
  public String createRepoNameFromComponentName(String projectKey, String componentName);
}
