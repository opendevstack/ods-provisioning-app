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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.bitbucket.Webhook;
import org.opendevstack.provision.util.GitUrlWrangler;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to interact with Bitbucket and to create projects and repositories
 *
 * @author Brokmeier, Pascal
 * @author utschig
 */
@Service
public class BitbucketAdapter extends BaseServiceAdapter implements ISCMAdapter {

  public BitbucketAdapter() {
    super("bitbucket");
  }

  private static final Logger logger = LoggerFactory.getLogger(BitbucketAdapter.class);

  @Value("${bitbucket.api.path}")
  private String bitbucketApiPath;

  @Value("${bitbucket.uri}")
  private String bitbucketUri;

  @Value("${atlassian.domain}")
  private String confluenceDomain;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Value("${openshift.jenkins.webhookproxy.name.pattern}")
  private String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.jenkins.trigger.secret}")
  private String projectOpenshiftJenkinsTriggerSecret;

  @Value("${bitbucket.default.user.group}")
  private String defaultUserGroup;

  @Value("${bitbucket.technical.user}")
  private String technicalUser;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Value("${idmanager.group.opendevstack-users}")
  private String openDevStackUsersGroupName;

  @Value("${provision.scm.grant.repository.writetoeveryuser:false}")
  private boolean grantRepositoryWriteToAllOpenDevStackUsers;

  @Autowired IODSAuthnzAdapter manager;

  private static final String PROJECT_PATTERN = "%s%s/projects";

  private static final String ID_GROUPS = "groups";
  private static final String ID_USERS = "users";

  public enum PROJECT_PERMISSIONS {
    PROJECT_ADMIN,
    PROJECT_WRITE,
    PROJECT_READ
  }

  public enum REPOSITORY_PERMISSIONS {
    REPO_ADMIN,
    REPO_WRITE
  }

  public String createSCMProjectForODSProject(OpenProjectData project) throws IOException {
    BitbucketProjectData data = callCreateProjectApi(project);

    project.scmvcsUrl = data.getLinks().get("self").get(0).getHref();
    return project.scmvcsUrl;
  }

  @SuppressWarnings("squid:S3776")
  @Override
  public Map<String, Map<URL_TYPE, String>> createComponentRepositoriesForODSProject(
      OpenProjectData project) throws IOException {
    Map<String, Map<URL_TYPE, String>> createdRepositories = new HashMap<>();

    if (project.quickstarters != null) {

      logger.debug(
          "Project {} - new quickstarters: {}", project.projectKey, project.quickstarters.size());

      for (Map<String, String> option : project.quickstarters) {
        logger.debug(
            "Creating repo for quickstarters: {}  in {}",
            option.get(OpenProjectData.COMPONENT_ID_KEY),
            project.projectKey);

        String repoName =
            createRepoNameFromComponentName(
                project.projectKey, option.get(OpenProjectData.COMPONENT_ID_KEY));

        Repository repo = new Repository();
        repo.setName(repoName);

        if (project.specialPermissionSet) {
          repo.setAdminGroup(project.projectAdminGroup);
          repo.setUserGroup(project.projectUserGroup);
        } else {
          repo.setAdminGroup(this.defaultUserGroup);
          repo.setUserGroup(this.defaultUserGroup);
        }

        Map<URL_TYPE, String> componentRepository = null;

        try {
          RepositoryData result = callCreateRepoApi(project.projectKey, repo);
          createWebHooksForRepository(result, project);

          componentRepository = result.convertRepoToOpenDataProjectRepo();

          GitUrlWrangler gitUrlWrangler = new GitUrlWrangler();

          String gitSSHUrl = componentRepository.get(URL_TYPE.URL_CLONE_SSH);

          if (gitSSHUrl != null) {
            gitSSHUrl = gitUrlWrangler.buildGitUrl(manager.getUserName(), technicalUser, gitSSHUrl);

            componentRepository.put(URL_TYPE.URL_CLONE_SSH, gitSSHUrl);
          }

          String gitHttpUrl = componentRepository.get(URL_TYPE.URL_CLONE_HTTP);

          componentRepository.put(URL_TYPE.URL_CLONE_HTTP, gitHttpUrl);
        } catch (IOException ex) {
          logger.error(
              "Error in creating repo: " + option.get(OpenProjectData.COMPONENT_ID_KEY), ex);
          throw new IOException(
              "Error in creating repo: "
                  + option.get(OpenProjectData.COMPONENT_ID_KEY)
                  + "\n"
                  + "details: "
                  + ex.getMessage());
        }
        createdRepositories.put(repoName, componentRepository);
      }
    }

    return createdRepositories;
  }

  @Override
  public Map<String, Map<URL_TYPE, String>> createAuxiliaryRepositoriesForODSProject(
      OpenProjectData project, String[] auxiliaryRepos) {
    Map<String, Map<URL_TYPE, String>> repositories = new HashMap<>();
    for (String name : auxiliaryRepos) {
      Repository repo = new Repository();
      String repoName = createRepoNameFromComponentName(project.projectKey, name);
      repo.setName(repoName);

      if (project.specialPermissionSet) {
        repo.setAdminGroup(project.projectAdminGroup);
        repo.setUserGroup(project.projectUserGroup);
      } else {
        repo.setAdminGroup(this.globalKeyuserRoleName);
        repo.setUserGroup(this.defaultUserGroup);
      }

      try {
        RepositoryData result = callCreateRepoApi(project.projectKey, repo);
        repositories.put(result.getName(), result.convertRepoToOpenDataProjectRepo());
      } catch (IOException ex) {
        logger.error("Error in creating auxiliary repo", ex);
      }
    }
    return repositories;
  }

  // Create webhook for CI (using webhook proxy)
  protected void createWebHooksForRepository(RepositoryData repo, OpenProjectData project) {

    // projectOpenshiftJenkinsWebhookProxyNamePattern is e.g.
    // "webhook-proxy-%s-cd%s"
    String webhookProxyHost =
        String.format(
            projectOpenshiftJenkinsWebhookProxyNamePattern,
            project.projectKey.toLowerCase(),
            projectOpenshiftBaseDomain);
    String triggerSecret =
        project.webhookProxySecret != null
            ? project.webhookProxySecret
            : projectOpenshiftJenkinsTriggerSecret;
    String webhookProxyUrl = "https://" + webhookProxyHost + "?trigger_secret=" + triggerSecret;
    Webhook webhook = new Webhook();
    webhook.setName("Jenkins");
    webhook.setActive(true);
    webhook.setUrl(webhookProxyUrl);
    List<String> events = new ArrayList<>();
    events.add("repo:refs_changed");
    events.add("pr:merged");
    events.add("pr:declined");
    webhook.setEvents(events);

    // projects/CLE200/repos/cle200-be-node-express/webhooks
    String url =
        String.format(
            "%s/%s/repos/%s/webhooks", getAdapterApiUri(), project.projectKey, repo.getSlug());

    try {
      // restClient.callHttp(url, webhook, false, RestClient.HTTP_VERB.POST, Webhook.class);
      RestClientCall call = httpPost().url(url).body(webhook).returnType(Webhook.class);
      restClient.execute(call);
      logger.info("created hook: {}", webhook.getUrl());
    } catch (IOException ex) {
      logger.error("Error in webhook call", ex);
    }
  }

  protected BitbucketProjectData callCreateProjectApi(OpenProjectData project) throws IOException {
    BitbucketProject bbProject = createBitbucketProject(project);

    // BitbucketProjectData projectData =
    //        restClient.callHttp(getAdapterApiUri(), bbProject, false, RestClient.HTTP_VERB.POST,
    // BitbucketProjectData.class);
    RestClientCall call =
        httpPost().url(getAdapterApiUri()).body(bbProject).returnType(BitbucketProjectData.class);
    BitbucketProjectData projectData = restClient.execute(call);
    if (project.specialPermissionSet) {
      setProjectPermissions(
          projectData, ID_GROUPS, globalKeyuserRoleName, PROJECT_PERMISSIONS.PROJECT_ADMIN);
      setProjectPermissions(
          projectData, ID_GROUPS, project.projectAdminGroup, PROJECT_PERMISSIONS.PROJECT_ADMIN);
      setProjectPermissions(
          projectData, ID_GROUPS, project.projectUserGroup, PROJECT_PERMISSIONS.PROJECT_WRITE);
      setProjectPermissions(
          projectData, ID_GROUPS, project.projectReadonlyGroup, PROJECT_PERMISSIONS.PROJECT_READ);
    } else {
      setProjectPermissions(
          projectData, ID_GROUPS, defaultUserGroup, PROJECT_PERMISSIONS.PROJECT_WRITE);
      setProjectPermissions(
          projectData, ID_GROUPS, openDevStackUsersGroupName, PROJECT_PERMISSIONS.PROJECT_READ);
    }

    String projectCdUser = technicalUser;
    if (project.cdUser != null && !project.cdUser.trim().isEmpty()) {
      projectCdUser = project.cdUser;
    }
    // set the technical user in any case
    setProjectPermissions(projectData, ID_USERS, projectCdUser, PROJECT_PERMISSIONS.PROJECT_WRITE);

    return projectData;
  }

  protected RepositoryData callCreateRepoApi(String projectKey, Repository repo)
      throws IOException {
    String path = String.format("%s/%s/repos", getAdapterApiUri(), projectKey);

    // RepositoryData data = restClient.callHttp(path, repo, false, RestClient.HTTP_VERB.POST,
    // RepositoryData.class);
    RepositoryData data =
        restClient.execute(httpPost().url(path).body(repo).returnType(RepositoryData.class));
    if (data == null) {
      throw new IOException(
          String.format(
              "Repo %s, for project %s could not be created"
                  + " - no response from endpoint, please check logs",
              repo.getName(), projectKey));
    }
    setRepositoryAdminPermissions(data, projectKey, ID_GROUPS, repo.getUserGroup());
    setRepositoryAdminPermissions(data, projectKey, ID_USERS, technicalUser);
    if (grantRepositoryWriteToAllOpenDevStackUsers) {
      logger.info(
          "Grant write to every member of {} to repository {}",
          openDevStackUsersGroupName,
          data.getSlug());
      setRepositoryPermissions(
          data,
          projectKey,
          ID_GROUPS,
          openDevStackUsersGroupName,
          REPOSITORY_PERMISSIONS.REPO_WRITE);
    }
    return data;
  }

  protected void setProjectPermissions(
      BitbucketProjectData data,
      String pathFragment,
      String groupOrUser,
      PROJECT_PERMISSIONS rights)
      throws IOException {
    String basePath = getAdapterApiUri();
    String url = String.format("%s/%s/permissions/%s", basePath, data.getKey(), pathFragment);

    // restClient.callHttp(url, permissions, true, RestClient.HTTP_VERB.PUT, String.class);
    Map<String, String> header = new HashMap<>();
    header.put("Content-Type", "application/json");
    restClient.execute(
        httpPut()
            .url(url)
            .body("")
            .queryParams(buildPermissionQueryParams(rights.toString(), groupOrUser))
            .returnType(String.class));
  }

  protected void setRepositoryAdminPermissions(
      RepositoryData data, String key, String userOrGroup, String userOrGroupName)
      throws IOException {
    setRepositoryPermissions(
        data, key, userOrGroup, userOrGroupName, REPOSITORY_PERMISSIONS.REPO_ADMIN);
  }

  private void setRepositoryPermissions(
      RepositoryData data,
      String key,
      String userOrGroup,
      String userOrGroupName,
      REPOSITORY_PERMISSIONS permission)
      throws IOException {
    String basePath = getAdapterApiUri();
    String url =
        String.format("%s/%s/repos/%s/permissions/%s", basePath, key, data.getSlug(), userOrGroup);

    // restClient.callHttp(url, permissions, true, RestClient.HTTP_VERB.PUT, String.class);
    restClient.execute(
        httpPut()
            .url(url)
            .body("")
            .queryParams(buildPermissionQueryParams(permission.toString(), userOrGroupName))
            .returnType(String.class));
  }

  private Map<String, String> buildPermissionQueryParams(String permission, String groupOrUser) {
    Map<String, String> permissions = new HashMap<>();
    permissions.put("permission", permission);
    permissions.put("name", groupOrUser);
    return permissions;
  }

  static BitbucketProject createBitbucketProject(OpenProjectData jiraProject) {
    BitbucketProject project = new BitbucketProject();
    project.setKey(jiraProject.projectKey);
    project.setName(jiraProject.projectName);
    project.setDescription((jiraProject.description != null) ? jiraProject.description : "");
    return project;
  }

  /**
   * Get the bitbucket http endpoint
   *
   * @return the endpoint - cant be null
   */
  @Override
  public String getAdapterApiUri() {
    return String.format(PROJECT_PATTERN, bitbucketUri, bitbucketApiPath);
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException();
  }

  @Override
  public String createRepoNameFromComponentName(String projectKey, String componentName) {
    return String.format("%s-%s", projectKey, componentName).toLowerCase().replace('_', '-');
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    /*
     * TODO - I suggest we leave the repos as is .. to NOT touch any code in the remove quickstarter
     * phase
     */
    if (project.repositories == null || project.repositories.size() == 0) {
      logger.debug("Project {} not affected from cleanup, no repos", project.projectKey);
      return leftovers;
    }

    Set<String> repositoryNames = project.repositories.keySet();

    logger.debug("Cleanup of {} scm repositories", repositoryNames);

    int failedRepoCleanup = 0;

    for (String repoName : repositoryNames) {
      try {
        String repoPath =
            String.format("%s/%s/repos/%s", getAdapterApiUri(), project.projectKey, repoName);
        // restClient.callHttp(repoPath, null, false, RestClient.HTTP_VERB.DELETE, null);
        restClient.execute(httpDelete().url(repoPath).returnType(null));
        logger.debug("Removed scm repo {}", repoName);
      } catch (Exception eCreateRepo) {
        logger.debug("Could not remove repo {}, error {}", repoName, eCreateRepo.getMessage());
        failedRepoCleanup++;
      }
    }
    if (failedRepoCleanup > 0) {
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.SCM_REPO, failedRepoCleanup);
    }
    if (stage.equals(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION)) {
      logger.debug(
          "Cleanup of repos done - status: {} " + "components are left ..",
          leftovers.size() == 0 ? 0 : leftovers);

      return leftovers;
    }

    logger.debug("Starting scm project cleanup with url {}", project.scmvcsUrl);

    String projectPath = String.format("%s/%s", getAdapterApiUri(), project.projectKey);

    try {
      // restClient.callHttp(projectPath, null, false, RestClient.HTTP_VERB.DELETE, null);
      restClient.execute(httpDelete().url(projectPath).returnType(null));
    } catch (Exception eProjectDelete) {
      logger.debug(
          "Could not remove project {}, error {}", project.projectKey, eProjectDelete.getMessage());
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.SCM_PROJECT, 1);
    }

    project.scmvcsUrl = null;

    logger.debug(
        "Cleanup done - status: {} components are left ..", leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }
}
