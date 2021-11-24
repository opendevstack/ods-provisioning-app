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
package org.opendevstack.provision.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;

/** Open Plugin API to create and update projects */
public class OpenProjectData {

  /** The key to get the chosen name for the quickstarter */
  public static final String COMPONENT_ID_KEY = "component_id";
  /** The key to get the type of the quickstarter */
  public static final String COMPONENT_TYPE_KEY = "component_type";
  /** The quickstarters type as description */
  public static final String COMPONENT_DESC_KEY = "component_description";
  /** The unique name of the project, must not be null */
  private String projectName = null;
  /** Description of the project, can be null */
  private String description = null;
  /** The unique key of the project, must not be null */
  private String projectKey = null;
  /** The secret to call webhook_proxy within the new env. Needed 2 create quickstarters */
  private String webhookProxySecret = null;
  /** The project specific cd-user */
  private String cdUser = null;
  /**
   * Map of quickstarters used, to get the chosen name of the quickstarter picked, use {@link
   * #COMPONENT_ID_KEY} against the map contained. To get the quickstarter's type use {@link
   * #COMPONENT_TYPE_KEY}
   */
  private List<Map<String, String>> quickstarters = null;

  /** create spaces thru {@link IBugtrackerAdapter} and {@link ICollaborationAdapter} */
  private boolean bugtrackerSpace = true;
  /** Create Platform projects thru {@link IJobExecutionAdapter} */
  private boolean platformRuntime = true;
  /** The url of the bugtracker project */
  private String bugtrackerUrl = null;
  /** The url of the collaboration space */
  private String collaborationSpaceUrl = null;
  /** The url of the SCM project */
  private String scmvcsUrl = null;
  /**
   * Created project repositories. The key denotes the repository name, the contained map contains
   * the repository links (urls)
   */
  private Map<String, Map<URL_TYPE, String>> repositories = null;
  /** The url of the jenkins / build engine */
  private String platformBuildEngineUrl = null;
  /** The url of the cd environment */
  private String platformCdEnvironmentUrl = null;
  /** The url of the dev environment */
  private String platformDevEnvironmentUrl = null;
  /** The url of the test environment */
  private String platformTestEnvironmentUrl = null;

  // permissions
  /** The admin group - with admin rights to the project */
  private String projectAdminGroup = null;
  /** the user group, needs WRITE access to repositories */
  private String projectUserGroup = null;
  /** The admin user of the spaces / projects created never NULL */
  private String projectAdminUser = null;
  /** Name of the readonly group, can be null */
  private String projectReadonlyGroup = null;

  /** Create a permission set within the spaces / projects / repositories */
  private boolean specialPermissionSet = false;

  private Integer specialPermissionSchemeId;

  /**
   * The last jobs that where triggered by {@link
   * IJobExecutionAdapter#createPlatformProjects(OpenProjectData)} or {@link
   * IJobExecutionAdapter#provisionComponentsBasedOnQuickstarters(OpenProjectData)}
   */
  @JsonIgnoreProperties({"lastExecutionJobs"})
  private List<ExecutionJob> lastExecutionJobs = null;

  /** The type of project(s) that should be created, used for templating */
  private String projectType = null;

  /** The storage path location */
  private String physicalLocation = null;

  private String projectRoleForAdminGroup;
  private String projectRoleForUserGroup;
  private String projectRoleForReadonlyGroup;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectKey == null) ? 0 : projectKey.hashCode());
    result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
    return result;
  }

  public List<Map<String, String>> getQuickstarters() {
    return quickstarters == null ? Collections.emptyList() : quickstarters;
  }

  public List<Map<String, String>> removeQuickstartersFromProject(
      List<Map<String, String>> quickstartersToRemove) {
    if (quickstartersToRemove == null) {
      return this.quickstarters;
    }
    for (Map<String, String> quickStarterToRemove : quickstartersToRemove) {
      if (quickStarterToRemove.get(COMPONENT_ID_KEY) == null) {
        throw new NullPointerException("Cannot delete quickstarter with id null!");
      }
      // loop over the currently provisioned quickstarters, and find the
      // one with similar id - to then remove it.
      List<Map<String, String>> currentQuickstarters = getQuickstarters();
      for (int i = 0; i < currentQuickstarters.size(); i++) {
        Map<String, String> currentQuickstarter = currentQuickstarters.get(i);
        if (currentQuickstarter
            .get(COMPONENT_ID_KEY)
            .equalsIgnoreCase(quickStarterToRemove.get(COMPONENT_ID_KEY))) {
          quickstarters.remove(i);
        }
      }
    }
    return quickstarters;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    OpenProjectData other = (OpenProjectData) obj;

    if (projectKey == null) {
      if (other.projectKey != null) {
        return false;
      }
    } else if (!projectKey.equals(other.projectKey)) {
      return false;
    }

    if (projectName == null) {
      if (other.projectName != null) {
        return false;
      }
    } else if (!projectName.equals(other.projectName)) {
      return false;
    }

    return true;
  }

  /** @return empty list if property specialPermissionSet is equal false. */
  public Set<String> specialPermissionSetGroups() {

    if (Boolean.FALSE.equals(specialPermissionSet)) {
      return Collections.EMPTY_SET;
    }

    Set<String> groups = new HashSet<>();

    if (projectAdminGroup != null && !projectAdminGroup.isEmpty()) {
      groups.add(projectAdminGroup);
    }

    if (projectUserGroup != null && !projectUserGroup.isEmpty()) {
      groups.add(projectUserGroup);
    }

    if (projectReadonlyGroup != null && !projectReadonlyGroup.isEmpty()) {
      groups.add(projectReadonlyGroup);
    }

    return Collections.unmodifiableSet(groups);
  }

  public String getProjectType() {
    return projectType;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  public void setSpecialPermissionSchemeId(Integer permissionSchemeId) {
    this.specialPermissionSchemeId = permissionSchemeId;
  }

  public Integer getSpecialPermissionSchemeId() {
    return specialPermissionSchemeId;
  }

  public String getProjectRoleForAdminGroup() {
    return projectRoleForAdminGroup;
  }

  public void setProjectRoleForAdminGroup(String projectRoleForAdminGroup) {
    this.projectRoleForAdminGroup = projectRoleForAdminGroup;
  }

  public String getProjectRoleForUserGroup() {
    return projectRoleForUserGroup;
  }

  public void setProjectRoleForUserGroup(String projectRoleForUserGroup) {
    this.projectRoleForUserGroup = projectRoleForUserGroup;
  }

  public String getProjectRoleForReadonlyGroup() {
    return projectRoleForReadonlyGroup;
  }

  public void setProjectRoleForReadonlyGroup(String projectRoleForReadonlyGroup) {
    this.projectRoleForReadonlyGroup = projectRoleForReadonlyGroup;
  }

  public String getProjectAdminGroup() {
    return projectAdminGroup;
  }

  public void setProjectAdminGroup(String projectAdminGroup) {
    this.projectAdminGroup = projectAdminGroup;
  }

  public String getProjectUserGroup() {
    return projectUserGroup;
  }

  public void setProjectUserGroup(String projectUserGroup) {
    this.projectUserGroup = projectUserGroup;
  }

  public String getProjectAdminUser() {
    return projectAdminUser;
  }

  public void setProjectAdminUser(String projectAdminUser) {
    this.projectAdminUser = projectAdminUser;
  }

  public String getProjectReadonlyGroup() {
    return projectReadonlyGroup;
  }

  public void setProjectReadonlyGroup(String projectReadonlyGroup) {
    this.projectReadonlyGroup = projectReadonlyGroup;
  }

  public boolean isSpecialPermissionSet() {
    return specialPermissionSet;
  }

  public void setSpecialPermissionSet(boolean specialPermissionSet) {
    this.specialPermissionSet = specialPermissionSet;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getWebhookProxySecret() {
    return webhookProxySecret;
  }

  public void setWebhookProxySecret(String webhookProxySecret) {
    this.webhookProxySecret = webhookProxySecret;
  }

  public String getCdUser() {
    return cdUser;
  }

  public void setCdUser(String cdUser) {
    this.cdUser = cdUser;
  }

  public void setQuickstarters(List<Map<String, String>> quickstarters) {
    this.quickstarters = quickstarters;
  }

  public boolean isBugtrackerSpace() {
    return bugtrackerSpace;
  }

  public void setBugtrackerSpace(boolean bugtrackerSpace) {
    this.bugtrackerSpace = bugtrackerSpace;
  }

  public boolean isPlatformRuntime() {
    return platformRuntime;
  }

  public void setPlatformRuntime(boolean platformRuntime) {
    this.platformRuntime = platformRuntime;
  }

  public String getBugtrackerUrl() {
    return bugtrackerUrl;
  }

  public void setBugtrackerUrl(String bugtrackerUrl) {
    this.bugtrackerUrl = bugtrackerUrl;
  }

  public String getCollaborationSpaceUrl() {
    return collaborationSpaceUrl;
  }

  public void setCollaborationSpaceUrl(String collaborationSpaceUrl) {
    this.collaborationSpaceUrl = collaborationSpaceUrl;
  }

  public String getScmvcsUrl() {
    return scmvcsUrl;
  }

  public void setScmvcsUrl(String scmvcsUrl) {
    this.scmvcsUrl = scmvcsUrl;
  }

  public Map<String, Map<URL_TYPE, String>> getRepositories() {
    return repositories;
  }

  public void setRepositories(Map<String, Map<URL_TYPE, String>> repositories) {
    this.repositories = repositories;
  }

  public String getPlatformBuildEngineUrl() {
    return platformBuildEngineUrl;
  }

  public void setPlatformBuildEngineUrl(String platformBuildEngineUrl) {
    this.platformBuildEngineUrl = platformBuildEngineUrl;
  }

  public String getPlatformCdEnvironmentUrl() {
    return platformCdEnvironmentUrl;
  }

  public void setPlatformCdEnvironmentUrl(String platformCdEnvironmentUrl) {
    this.platformCdEnvironmentUrl = platformCdEnvironmentUrl;
  }

  public String getPlatformDevEnvironmentUrl() {
    return platformDevEnvironmentUrl;
  }

  public void setPlatformDevEnvironmentUrl(String platformDevEnvironmentUrl) {
    this.platformDevEnvironmentUrl = platformDevEnvironmentUrl;
  }

  public String getPlatformTestEnvironmentUrl() {
    return platformTestEnvironmentUrl;
  }

  public void setPlatformTestEnvironmentUrl(String platformTestEnvironmentUrl) {
    this.platformTestEnvironmentUrl = platformTestEnvironmentUrl;
  }

  public List<ExecutionJob> getLastExecutionJobs() {
    return lastExecutionJobs;
  }

  public void setLastExecutionJobs(List<ExecutionJob> lastExecutionJobs) {
    this.lastExecutionJobs = lastExecutionJobs;
  }

  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }

  public String getPhysicalLocation() {
    return physicalLocation;
  }

  public void setPhysicalLocation(String physicalLocation) {
    this.physicalLocation = physicalLocation;
  }
}
