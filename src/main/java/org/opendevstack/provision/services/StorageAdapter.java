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

import com.google.common.base.Preconditions;
import java.util.*;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.IStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service to interact with the underlying storage system to liast the project history
 */
@Service
public class StorageAdapter implements IServiceAdapter {

  @Autowired IStorage storage;

  private static final Logger logger = LoggerFactory.getLogger(StorageAdapter.class);

  public Map<String, OpenProjectData> listProjectHistory() {
    Map<String, OpenProjectData> allProjects = storage.listProjectHistory();
    Map<String, OpenProjectData> filteredAndOrderedProjects = new TreeMap<>();

    for (Map.Entry<String, OpenProjectData> project : allProjects.entrySet()) {
      OpenProjectData projectData = project.getValue();
      logger.debug(
          "Project: {} groups: {},{} - permissioned? {}",
          projectData.projectKey,
          projectData.projectAdminGroup,
          projectData.projectUserGroup,
          projectData.specialPermissionSet);

      if (!projectData.specialPermissionSet) {
        filteredAndOrderedProjects.put(projectData.projectKey, projectData);
      } else {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
          if (authority.getAuthority().equalsIgnoreCase(projectData.projectAdminGroup)
              || authority.getAuthority().equalsIgnoreCase(projectData.projectUserGroup)) {
            filteredAndOrderedProjects.put(projectData.projectKey, projectData);
            break;
          }
        }
      }
    }

    return filteredAndOrderedProjects;
  }

  public AboutChangesData listAboutChangesData() {
    return storage.listAboutChangesData();
  }

  void setStorage(IStorage storage) {
    this.storage = storage;
  }

  public Map<String, OpenProjectData> getProjects() {
    Collection<OpenProjectData> projects = listProjectHistory().values();

    Map<String, OpenProjectData> map = new HashMap<>();

    for (OpenProjectData fProject : projects) {
      map.put(fProject.projectKey, fProject);
    }

    return map;
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    Collection<OpenProjectData> projectList = listProjectHistory().values();

    Map<String, String> result = new HashMap<>();

    for (OpenProjectData fProject : projectList) {
      if (filter.equalsIgnoreCase(fProject.projectKey)) {
        result.put(fProject.projectKey, fProject.description);
      }
    }
    return result;
  }

  @Override
  public String getAdapterApiUri() {
    return storage.getStoragePath();
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    if (!stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION)) {
      logger.debug("Project {} not affected from cleanup", project.projectKey);
      return leftovers;
    } else {
      OpenProjectData toBeDeleted = storage.getProject(project.projectKey);

      if (toBeDeleted == null) {
        logger.debug(
            "Project {} not affected from cleanup, " + "as it was never stored",
            project.projectKey);
        return leftovers;
      }

      boolean deleted = storage.deleteProject(toBeDeleted);

      if (!deleted) {
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PROJECT_DB, 1);
      }
    }

    logger.debug(
        "Cleanup done - status: {} components are left ..", leftovers.size() == 0 ? 0 : leftovers);

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

  @Override
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {
    throw new UnsupportedOperationException("not implemented yet!");
  }
}
