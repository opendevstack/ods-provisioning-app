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

package org.opendevstack.provision.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter.CLEANUP_LEFTOVER_COMPONENTS;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.IStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import com.google.common.base.Preconditions;

/**
 * Service to interact with the underlying storage system to liast the project history
 *
 * @author Torsten Jaeschke
 */

@Service
public class StorageAdapter implements IServiceAdapter {

  @Autowired
  IStorage storage;

  @Autowired
  IODSAuthnzAdapter authManager;

  private static final Logger logger = LoggerFactory.getLogger(StorageAdapter.class);

  public Map<String, OpenProjectData> listProjectHistory() {
    Map<String, OpenProjectData> allProjects = storage.listProjectHistory();
    Map<String, OpenProjectData> filteredProjects = new HashMap<>();

    Collection<GrantedAuthority> authorities = authManager.getAuthorities();
    logger.debug("User: {} Authorities: {}", authManager.getUserName(), authorities);

    for (Map.Entry<String, OpenProjectData> project : allProjects.entrySet()) {
      OpenProjectData projectData = project.getValue();
      logger.debug("Project: {} groups: {},{} - permissioned? {}", projectData.projectKey,
          projectData.projectAdminGroup, projectData.projectUserGroup,
          projectData.specialPermissionSet);

      if (!projectData.specialPermissionSet) {
        filteredProjects.put(projectData.projectKey, projectData);
      } else {
        for (GrantedAuthority authority : authorities) {
          if (authority.getAuthority().equalsIgnoreCase(projectData.projectAdminGroup)
              || authority.getAuthority().equalsIgnoreCase(projectData.projectUserGroup)) {
            filteredProjects.put(projectData.projectKey, projectData);
            break;
          }
        }
      }
    }

    return filteredProjects;
  }

  public AboutChangesData listAboutChangesData() {
    return storage.listAboutChangesData();
  }

  void setStorage(IStorage storage) {
    this.storage = storage;
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    Collection<OpenProjectData> filteredProjects = listProjectHistory().values();

    Map<String, String> filteredKeys = new HashMap<>();

    for (OpenProjectData fProject : filteredProjects) {
      if (filter.equalsIgnoreCase(fProject.projectKey)) {
        filteredKeys.put(fProject.projectKey, fProject.description);
      }
    }
    return filteredKeys;
  }

  @Override
  public String getAdapterApiUri() {
    return storage.getStoragePath();
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(LIFECYCLE_STAGE stage,
      OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    if (!stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION)) {
      logger.debug("Project {} not affected from cleanup", project.projectKey);
      return leftovers;
    } else {
      OpenProjectData toBeDeleted = storage.getProject(project.projectKey);

      if (toBeDeleted == null) {
        logger.debug("Project {} not affected from cleanup, " + "as it was never stored",
            project.projectKey);
        return leftovers;
      }

      boolean deleted = storage.deleteProject(toBeDeleted);

      if (!deleted) {
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PROJECT_DB, 1);
      }
    }

    logger.debug("Cleanup done - status: {} components are left ..",
        leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }

  public OpenProjectData getFilteredSingleProject(String projectkey) {
    Preconditions.checkNotNull(projectkey, "Cannot find null project");

    // we use the filtering here to enforce security
    Collection<OpenProjectData> userProjects = listProjectHistory().values();

    for (OpenProjectData fProject : userProjects) {
      if (projectkey.equalsIgnoreCase(fProject.projectKey)) {
        return fProject;
      }
    }
    return null;
  }
}
