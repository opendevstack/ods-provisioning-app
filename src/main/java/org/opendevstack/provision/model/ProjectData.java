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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.bitbucket.Link;

/** ProjectData */
@SuppressWarnings("common-java:DuplicatedBlocks")
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
// TODO remove deprecated class
public class ProjectData {
  /** The unique name of the project, must not be null */
  public String name = null;
  /** Description of the project, can be null */
  public String description = null;
  /** The unique key of the project, must not be null */
  public String key = null;

  public List<Map<String, String>> quickstart = null;
  /** create spaces thru {@link IBugtrackerAdapter} and {@link ICollaborationAdapter} */
  public boolean jiraconfluencespace = true;
  /** Create Platform projects thru {@link IJobExecutionAdapter} */
  public boolean openshiftproject = true;
  /** The url of the bugtracker project */
  public String jiraUrl = null;
  /** The url of the collaboration space */
  public String confluenceUrl = null;
  /** The url of the SCM project */
  public String bitbucketUrl = null;

  public Map<String, Map<String, List<Link>>> repositories = null;
  /** The url of the jenkins / build engine */
  public String openshiftJenkinsUrl = null;
  /** The url of the dev environment */
  public String openshiftConsoleDevEnvUrl = null;
  /** The url of the test environment */
  public String openshiftConsoleTestEnvUrl = null;

  // permissions
  /** The admin group - with admin rights to the project */
  public String adminGroup = null;
  /** the user group, needs WRITE access to repositories */
  public String userGroup = null;
  /** The admin user of the spaces / projects created never NULL */
  public String admin = null;
  /** Name of the readonly group, can be null */
  public String readonlyGroup = null;

  /** Create a permission set within the spaces / projects / repositories */
  public boolean createpermissionset = false;

  @JsonIgnoreProperties({"lastJobs"})
  public List<ExecutionJob> lastJobs = null;

  /** The type of project(s) that should be created, used for templating */
  public String projectType = null;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    ProjectData other = (ProjectData) obj;

    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    return true;
  }

  /**
   * Create a new Open project from this legacy
   *
   * @param project the legacy project
   * @return a project in the new strucutre
   */
  public static OpenProjectData toOpenProjectData(ProjectData project) {
    OpenProjectData open = new OpenProjectData();

    open.setProjectKey(project.key);
    open.setProjectName(project.name);
    open.setDescription(project.description);

    // roles & users
    open.setSpecialPermissionSet(project.createpermissionset);
    open.setProjectAdminUser(project.admin);
    open.setProjectAdminGroup(project.adminGroup);
    open.setProjectReadonlyGroup(project.readonlyGroup);
    open.setProjectUserGroup(project.userGroup);

    // quickstarters and repos

    Map<String, Map<String, List<Link>>> legacyRepos = project.repositories;

    Map<String, Map<URL_TYPE, String>> repositories = new HashMap<>();

    if (legacyRepos != null) {
      for (Map.Entry<String, Map<String, List<Link>>> legacyRepo : legacyRepos.entrySet()) {
        String repoName = legacyRepo.getKey();
        Map<URL_TYPE, String> newRepoLinks = new HashMap<>();
        Map<String, List<Link>> legacyRepLinks = legacyRepo.getValue();

        if (legacyRepLinks == null) {
          continue;
        }

        List<Link> cloneRepos = legacyRepLinks.get("clone");
        for (Link cloneRepoLink : cloneRepos) {
          if ("ssh".equals(cloneRepoLink.getName())) {
            newRepoLinks.put(URL_TYPE.URL_CLONE_SSH, cloneRepoLink.getHref());
          } else if ("http".equals(cloneRepoLink.getName())) {
            newRepoLinks.put(URL_TYPE.URL_CLONE_HTTP, cloneRepoLink.getHref());
          }
        }

        List<Link> selfRepos = legacyRepLinks.get("self");
        for (Link selfRepoLink : selfRepos) {
          newRepoLinks.put(URL_TYPE.URL_BROWSE_HTTP, selfRepoLink.getHref());
        }

        repositories.put(repoName, newRepoLinks);
      }
    }

    open.setRepositories(repositories);
    open.setQuickstarters(project.quickstart);

    // urls & config
    open.setBugtrackerSpace(project.jiraconfluencespace);
    open.setScmvcsUrl(project.bitbucketUrl);

    open.setBugtrackerUrl(project.jiraUrl);
    open.setCollaborationSpaceUrl(project.confluenceUrl);

    open.setPlatformRuntime(project.openshiftproject);
    open.setPlatformDevEnvironmentUrl(project.openshiftConsoleDevEnvUrl);
    open.setPlatformTestEnvironmentUrl(project.openshiftConsoleTestEnvUrl);
    open.setPlatformBuildEngineUrl(project.openshiftJenkinsUrl);

    // state
    open.setLastExecutionJobs(project.lastJobs);

    return open;
  }
}
