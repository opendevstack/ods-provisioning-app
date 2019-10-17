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
import java.util.List;
import java.util.Map;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;

/**
 * Open Plugin API to create and update projects
 *
 * @author utschig
 */
public class OpenProjectData {

  /** The key to get the chosen name for the quickstarter */
  public static final String COMPONENT_ID_KEY = "component_id";
  /** The key to get the type of the quickstarter */
  public static final String COMPONENT_TYPE_KEY = "component_type";
  /** The quickstarters type as description */
  public static final String COMPONENT_DESC_KEY = "component_description";
  /** The unique name of the project, must not be null */
  public String projectName = null;
  /** Description of the project, can be null */
  public String description = null;
  /** The unique key of the project, must not be null */
  public String projectKey = null;
  /**
   * Map of quickstarters used, to get the chosen name of the quickstarter picked, use {@link
   * #COMPONENT_ID_KEY} against the map contained. To get the quickstarter's type use {@link
   * #COMPONENT_TYPE_KEY}
   */
  public List<Map<String, String>> quickstarters = null;
  /** create spaces thru {@link IBugtrackerAdapter} and {@link ICollaborationAdapter} */
  public boolean bugtrackerSpace = true;
  /** Create Platform projects thru {@link IJobExecutionAdapter} */
  public boolean platformRuntime = true;
  /** The url of the bugtracker project */
  public String bugtrackerUrl = null;
  /** The url of the collaboration space */
  public String collaborationSpaceUrl = null;
  /** The url of the SCM project */
  public String scmvcsUrl = null;
  /**
   * Created project repositories. The key denotes the repository name, the contained map contains
   * the repository links (urls)
   */
  public Map<String, Map<URL_TYPE, String>> repositories = null;
  /** The url of the jenkins / build engine */
  public String platformBuildEngineUrl = null;
  /** The url of the dev environment */
  public String platformDevEnvironmentUrl = null;
  /** The url of the test environment */
  public String platformTestEnvironmentUrl = null;

  // permissions
  /** The admin group - with admin rights to the project */
  public String projectAdminGroup = null;
  /** the user group, needs WRITE access to repositories */
  public String projectUserGroup = null;
  /** The admin user of the spaces / projects created never NULL */
  public String projectAdminUser = null;
  /** Name of the readonly group, can be null */
  public String projectReadonlyGroup = null;

  /** Create a permission set within the spaces / projects / repositories */
  public boolean specialPermissionSet = false;

  /**
   * The last jobs that where triggered by {@link
   * IJobExecutionAdapter#createPlatformProjects(ProjectData, String)} or {@link
   * IJobExecutionAdapter#provisionComponentsBasedOnQuickstarters(ProjectData)}
   */
  @JsonIgnoreProperties({"lastExecutionJobs"})
  public List<String> lastExecutionJobs = null;

  /** The type of project(s) that should be created, used for templating */
  public String projectType = null;

  /** The storage path location */
  public String physicalLocation = null;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectKey == null) ? 0 : projectKey.hashCode());
    result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
    return result;
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
}
