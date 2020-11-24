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
import java.util.ArrayList;
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
   * Create a legacy project from a new one
   *
   * @param open the new style project pojo
   * @return the old style pojo
   */
  public static ProjectData fromOpenProjectData(OpenProjectData open) {
    ProjectData legacyProject = new ProjectData();

    legacyProject.key = open.projectKey;
    legacyProject.name = open.projectName;
    legacyProject.description = open.description;

    // roles & users
    legacyProject.createpermissionset = open.specialPermissionSet;
    legacyProject.admin = open.projectAdminUser;
    legacyProject.adminGroup = open.projectAdminGroup;
    legacyProject.readonlyGroup = open.projectReadonlyGroup;
    legacyProject.userGroup = open.projectUserGroup;

    // quickstarters and repos
    Map<String, Map<URL_TYPE, String>> openRepositories = open.repositories;

    Map<String, Map<String, List<Link>>> legacyRepositories = new HashMap<>();

    if (openRepositories != null) {
      for (Map.Entry<String, Map<URL_TYPE, String>> openRepo : openRepositories.entrySet()) {
        String openRepoName = openRepo.getKey();

        Map<URL_TYPE, String> openRepoLinks = openRepo.getValue();

        if (openRepoLinks == null) {
          continue;
        }

        List<Link> cloneLinks = new ArrayList<>();
        Link sshLink = new Link();
        sshLink.setName("ssh");
        sshLink.setHref(openRepoLinks.get(URL_TYPE.URL_CLONE_SSH));
        cloneLinks.add(sshLink);

        Link httpLink = new Link();
        httpLink.setName("http");
        httpLink.setHref(openRepoLinks.get(URL_TYPE.URL_CLONE_HTTP));
        cloneLinks.add(httpLink);

        Map<String, List<Link>> legacyRepoMap = new HashMap<>();
        legacyRepoMap.put("clone", cloneLinks);

        List<Link> browseLinks = new ArrayList<>();
        Link browseLink = new Link();
        browseLink.setName("null");
        browseLink.setHref(openRepoLinks.get(URL_TYPE.URL_BROWSE_HTTP));
        browseLinks.add(browseLink);

        legacyRepoMap.put("self", browseLinks);

        legacyRepositories.put(openRepoName, legacyRepoMap);
      }
    }

    legacyProject.repositories = legacyRepositories;

    legacyProject.quickstart = open.quickstarters;

    // urls & config
    legacyProject.jiraconfluencespace = open.bugtrackerSpace;
    legacyProject.jiraUrl = open.bugtrackerUrl;
    legacyProject.confluenceUrl = open.collaborationSpaceUrl;

    legacyProject.openshiftproject = open.platformRuntime;
    legacyProject.bitbucketUrl = open.scmvcsUrl;
    legacyProject.openshiftConsoleDevEnvUrl = open.platformDevEnvironmentUrl;
    legacyProject.openshiftConsoleTestEnvUrl = open.platformTestEnvironmentUrl;
    legacyProject.openshiftJenkinsUrl = open.platformBuildEngineUrl;

    // state
    legacyProject.lastJobs = open.lastExecutionJobs;

    return legacyProject;
  }

  /**
   * Create a new Open project from this legacy
   *
   * @param project the legacy project
   * @return a project in the new strucutre
   */
  public static OpenProjectData toOpenProjectData(ProjectData project) {
    OpenProjectData open = new OpenProjectData();

    open.projectKey = project.key;
    open.projectName = project.name;
    open.description = project.description;

    // roles & users
    open.specialPermissionSet = project.createpermissionset;
    open.projectAdminUser = project.admin;
    open.projectAdminGroup = project.adminGroup;
    open.projectReadonlyGroup = project.readonlyGroup;
    open.projectUserGroup = project.userGroup;

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

    open.repositories = repositories;
    open.quickstarters = project.quickstart;

    // urls & config
    open.bugtrackerSpace = project.jiraconfluencespace;
    open.scmvcsUrl = project.bitbucketUrl;

    open.bugtrackerUrl = project.jiraUrl;
    open.collaborationSpaceUrl = project.confluenceUrl;

    open.platformRuntime = project.openshiftproject;
    open.platformDevEnvironmentUrl = project.openshiftConsoleDevEnvUrl;
    open.platformTestEnvironmentUrl = project.openshiftConsoleTestEnvUrl;
    open.platformBuildEngineUrl = project.openshiftJenkinsUrl;

    // state
    open.lastExecutionJobs = project.lastJobs;

    return open;
  }
}
