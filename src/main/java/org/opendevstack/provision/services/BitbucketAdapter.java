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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.NotImplementedException;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.config.JenkinsPipelineProperties;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.bitbucket.Webhook;
import org.opendevstack.provision.properties.ScmGlobalProperties;
import org.opendevstack.provision.util.GitUrlWrangler;
import org.opendevstack.provision.util.exception.HttpException;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Service to interact with Bitbucket and to create projects and repositories */
@Service
public class BitbucketAdapter extends BaseServiceAdapter implements ISCMAdapter {

  public static final String GLOBAL_PERMISSION_PROJECT_CREATE = "PROJECT_CREATE";

  public static final String ADAPTER_NAME = "bitbucket";

  public static final String ADAPTER_CONFIGURATION_PREFIX = ADAPTER_NAME;

  public static final String FILTER_PARAM = "filter";

  private static final Logger logger = LoggerFactory.getLogger(BitbucketAdapter.class);

  @Value("${bitbucket.api.path}")
  private String bitbucketApiPath;

  @Value("${bitbucket.uri}")
  private String bitbucketUri;

  @Value("${atlassian.domain}")
  private String confluenceDomain;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
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

  @Value("${openshift.jenkins.project.webhookproxy.events}")
  private List<String> webhookEvents;

  @Autowired private ScmGlobalProperties scmGlobalProperties;

  @Autowired private IODSAuthnzAdapter manager;

  @Autowired private JenkinsPipelineProperties jenkinsPipelineProperties;

  private Set<String> noWebhookComponents;

  public static final String BITBUCKET_API_PROJECTS = "projects";
  public static final String BITBUCKET_API_GROUPS = "groups";
  public static final String BITBUCKET_API_USERS = "users";
  public static final String BITBUCKET_API_ADMIN_USERS = "admin/users";

  public static final String BASE_PATTERN = "%s%s/";
  public static final String BITBUCKET_API_PROJECTS_PATTERN = BASE_PATTERN + BITBUCKET_API_PROJECTS;
  public static final String BITBUCKET_API_PROJECT_PATTERN =
      BASE_PATTERN + BITBUCKET_API_PROJECTS + "/%s";
  public static final String BITBUCKET_API_GROUPS_PATTERN = BASE_PATTERN + BITBUCKET_API_GROUPS;
  public static final String BITBUCKET_API_USERS_PATTERN = BASE_PATTERN + BITBUCKET_API_USERS;
  public static final String BITBUCKET_API_ADMIN_USERS_PATTERN =
      BASE_PATTERN + BITBUCKET_API_ADMIN_USERS;

  private static final String ID_GROUPS = "groups";
  private static final String ID_USERS = "users";

  public enum PROJECT_PERMISSIONS {
    PROJECT_ADMIN,
    PROJECT_WRITE,
    PROJECT_READ
  }

  public enum REPOSITORY_PERMISSIONS {
    REPO_ADMIN,
    REPO_WRITE,
    REPO_READ
  }

  public BitbucketAdapter() {
    super(ADAPTER_CONFIGURATION_PREFIX);
  }

  @PostConstruct
  public void setupNoWebhookComponents() {
    var quickstarters = jenkinsPipelineProperties.getQuickstarter();
    noWebhookComponents =
        quickstarters.values().stream()
            .filter(qs -> !qs.isCreateWebhook())
            .map(qs -> qs.getName())
            .collect(Collectors.toSet());
    logger.info("noWebhookComponents={}", noWebhookComponents);
    logger.info("readablerepos={}", scmGlobalProperties.getReadableRepos());
  }

  public String createSCMProjectForODSProject(OpenProjectData project) throws IOException {
    BitbucketProjectData data = callCreateProjectApi(project);

    project.setScmvcsUrl(data.getLinks().get("self").get(0).getHref());
    return project.getScmvcsUrl();
  }

  @Override
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {

    try {
      Assert.notNull(newProject, "Parameter 'newProject' is null!");
      Assert.notNull(
          newProject.getProjectKey(), "Properties 'projectKey' of parameter 'newProject' is null!");

      logger.info(
          "checking create project preconditions for project '{}'!", newProject.getProjectKey());

      List<CheckPreconditionFailure> preconditionFailures =
          createProjectKeyExistsCheck(newProject.getProjectKey())
              .andThen(createUserHaveProjectCreateGlobalPermissionCheck(getUserName()))
              .andThen(createCheckGroupsExist(newProject))
              .andThen(createCheckUser(newProject))
              .apply(new ArrayList<>());

      logger.info(
          "done with check create project preconditions for project '{}'!",
          newProject.getProjectKey());

      return preconditionFailures;

    } catch (AdapterException e) {
      throw new CreateProjectPreconditionException(ADAPTER_NAME, newProject.getProjectKey(), e);
    } catch (Exception e) {
      String message =
          String.format(
              "Unexpected error when checking precondition for creation of project '%s'",
              newProject.getProjectKey());
      logger.error(message, e);
      throw new CreateProjectPreconditionException(
          ADAPTER_NAME, newProject.getProjectKey(), message);
    }
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createProjectKeyExistsCheck(String projectKey) {

    return preconditionFailures -> {
      logger.info("checking if project exists in bitbucket [projectKey={}]", projectKey);

      try {
        if (existsProject(projectKey)) {
          String message =
              String.format("project '%s' already exists in %s!", projectKey, ADAPTER_NAME);
          preconditionFailures.add(CheckPreconditionFailure.getProjectExistsInstance(message));
          logger.info(message);
        }
      } catch (IOException e) {
        throw new AdapterException(e);
      }

      return preconditionFailures;
    };
  }

  private boolean existsProject(String projectKey) throws IOException {

    JsonNode projectAsJson;
    try {
      projectAsJson = getProject(projectKey);
    } catch (HttpException e) {
      if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
        logger.debug("project '{}' was not found in {}!", projectKey, ADAPTER_NAME, e.getMessage());
        return false;
      } else {
        logger.warn("Unexpected method trying to get project '{}'!", projectKey, e);
        throw e;
      }
    }

    return projectKey.equalsIgnoreCase(projectAsJson.get("key").asText());
  }

  private JsonNode getProject(String projectKey) throws IOException {

    String url =
        String.format(BITBUCKET_API_PROJECT_PATTERN, bitbucketUri, bitbucketApiPath, projectKey);

    String response = getRestClient().execute(httpGet().url(url).returnType(String.class), true);
    Assert.notNull(response, "Response is null for '" + projectKey + "'");

    return new ObjectMapper().readTree(response);
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> createCheckUser(
      OpenProjectData project) {
    return preconditionFailures -> {

      // define cd user
      String user = technicalUser;
      // proof if CD user is project specific, if so, set read permissions to global read repos
      if (project.getCdUser() != null && !project.getCdUser().trim().isEmpty()) {
        user = project.getCdUser();
      }

      logger.info("checking if user '{}' exists!", user);

      if (!checkUserExists(user)) {
        String message = String.format("user '%s' does not exists in %s!", user, ADAPTER_NAME);
        preconditionFailures.add(CheckPreconditionFailure.getUnexistantUserInstance(message));
      }

      return preconditionFailures;
    };
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createCheckGroupsExist(OpenProjectData project) {
    return preconditionFailures -> {

      // check if groups exist
      // https://127.0.0.1/rest/api/1.0/groups?filter=EU-dBIX-administrators
      List<String> requiredGroups = new ArrayList<>();
      if (project.isSpecialPermissionSet()) {
        requiredGroups.add(globalKeyuserRoleName);
        requiredGroups.add(project.getProjectAdminGroup());
        requiredGroups.add(project.getProjectUserGroup());
        requiredGroups.add(project.getProjectReadonlyGroup());
      } else {
        requiredGroups.add(defaultUserGroup);
        requiredGroups.add(openDevStackUsersGroupName);
      }

      logger.info("checking if groups exists");

      requiredGroups.stream()
          .forEach(
              group -> {
                logger.debug("checking if group '{}' exists", group);
                try {
                  if (!checkGroupExists(group)) {
                    String message =
                        String.format("group '%s' does not exists in %s!", group, ADAPTER_NAME);
                    preconditionFailures.add(
                        CheckPreconditionFailure.getUnexistantGroupInstance(message));
                  }
                } catch (IOException e) {
                  throw new AdapterException(e);
                }
              });

      return preconditionFailures;
    };
  }

  /** https://127.0.0.1/rest/api/1.0/admin/users?filter={{user}} */
  private boolean checkUserExists(String username) {

    Map<String, String> params = new HashMap<>();
    params.put(FILTER_PARAM, username);

    String url = String.format(BITBUCKET_API_ADMIN_USERS_PATTERN, bitbucketUri, bitbucketApiPath);
    try {

      String response = null;
      try {
        response =
            getRestClient()
                .execute(httpGet().url(url).queryParams(params).returnType(String.class));
        Assert.notNull(response, "Response is null for '" + username + "'");
      } catch (HttpException e) {
        if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
          logger.debug("User '{}' was not found in {}!", username, ADAPTER_NAME, e);
          return false;
        } else {
          logger.warn("Unexpected method trying to get user '{}'!", username, e);
          throw e;
        }
      }

      JsonNode json = new ObjectMapper().readTree(response);

      return containsUser(json, username);

    } catch (IOException e) {
      throw new AdapterException(e);
    }
  }

  /**
   * https://127.0.0.1/rest/api/1.0/groups?filter=EU-dBIX-administrators
   *
   * @param groupName
   * @return
   * @throws IOException
   */
  private boolean checkGroupExists(String groupName) throws IOException {

    Map<String, String> params = new HashMap<>();
    params.put(FILTER_PARAM, groupName);

    String url = String.format(BITBUCKET_API_GROUPS_PATTERN, bitbucketUri, bitbucketApiPath);

    String response = null;
    try {
      response =
          getRestClient().execute(httpGet().url(url).queryParams(params).returnType(String.class));
      Assert.notNull(response, "Response is null for '" + groupName + "'");
    } catch (HttpException e) {
      if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
        logger.debug("Group '{}' was not found in {}!", groupName, ADAPTER_NAME, e);
        return false;
      } else {
        logger.warn("Unexpected method trying to get group '{}'!", groupName, e);
        throw e;
      }
    }

    JsonNode json = new ObjectMapper().readTree(response);

    return containsGroup(json, groupName);
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createUserHaveProjectCreateGlobalPermissionCheck(final String username) {
    return preconditionFailures -> {
      try {
        logger.info("checking user '{}' have project create global permission", username);

        Map<String, String> params = new HashMap<>();
        params.put(FILTER_PARAM, username);
        params.put("permission", GLOBAL_PERMISSION_PROJECT_CREATE);

        String url = String.format(BITBUCKET_API_USERS_PATTERN, bitbucketUri, bitbucketApiPath);
        String response =
            getRestClient()
                .execute(httpGet().url(url).queryParams(params).returnType(String.class));
        Assert.notNull(response, "Response is null for '" + username + "'");
        JsonNode json = new ObjectMapper().readTree(response);

        if (!containsUser(json, username)) {
          preconditionFailures.add(
              CheckPreconditionFailure.getGlobalCreateProjectBitBucketPermissionMissingInstance(
                  String.format(
                      "user '%s' does not have create project global permissions!", username)));
        }

        return preconditionFailures;

      } catch (IOException e) {
        logger.error(
            "failed to check if user '{}' have project create global permission", username, e);
        throw new AdapterException(e);
      }
    };
  }

  public boolean containsUser(JsonNode json, String username) {

    JsonNode values = json.get("values");
    if (json.get("size").asInt() < 1 || !values.isArray()) {
      return false;
    }

    List<String> users =
        json.get("values").findValues("name").stream()
            .map(jsonNode -> jsonNode.asText().toLowerCase())
            .filter(value -> username.equalsIgnoreCase(value))
            .collect(Collectors.toList());

    if (users.isEmpty() || users.size() > 1 || !users.contains(username.toLowerCase())) {
      return false;
    }

    return true;
  }

  public static boolean containsGroup(JsonNode json, String group) {

    JsonNode values = json.get("values");
    if (json.get("size").asInt() < 1 || !values.isArray()) {
      return false;
    }

    return group.equalsIgnoreCase(json.get("values").get(0).asText());
  }

  @SuppressWarnings("squid:S3776")
  @Override
  public Map<String, Map<URL_TYPE, String>> createComponentRepositoriesForODSProject(
      OpenProjectData project) throws IOException {
    Map<String, Map<URL_TYPE, String>> createdRepositories = new HashMap<>();

    if (project.getQuickstarters() != null) {

      logger.debug(
          "Project {} - new quickstarters: {}",
          project.getProjectKey(),
          project.getQuickstarters().size());

      for (Map<String, String> option : project.getQuickstarters()) {
        logger.debug(
            "Creating repo for quickstarters: {}  in {}",
            option.get(OpenProjectData.COMPONENT_ID_KEY),
            project.getProjectKey());

        String repoName =
            createRepoNameFromComponentName(
                project.getProjectKey(), option.get(OpenProjectData.COMPONENT_ID_KEY));

        Repository repo = new Repository();
        repo.setName(repoName);

        if (project.isSpecialPermissionSet()) {
          repo.setAdminGroup(project.getProjectAdminGroup());
          repo.setUserGroup(project.getProjectUserGroup());
        } else {
          repo.setAdminGroup(defaultUserGroup);
          repo.setUserGroup(defaultUserGroup);
        }

        Map<URL_TYPE, String> componentRepository = null;

        try {
          RepositoryData result = callCreateRepoApi(project.getProjectKey(), repo);
          createWebHooksForRepository(
              result, project, option.get(OpenProjectData.COMPONENT_TYPE_KEY));

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
      String repoName = createRepoNameFromComponentName(project.getProjectKey(), name);
      repo.setName(repoName);

      if (project.isSpecialPermissionSet()) {
        repo.setAdminGroup(project.getProjectAdminGroup());
        repo.setUserGroup(project.getProjectUserGroup());
      } else {
        repo.setAdminGroup(globalKeyuserRoleName);
        repo.setUserGroup(defaultUserGroup);
      }

      try {
        RepositoryData result = callCreateRepoApi(project.getProjectKey(), repo);
        repositories.put(result.getName(), result.convertRepoToOpenDataProjectRepo());
      } catch (IOException ex) {
        logger.error("Error in creating auxiliary repo", ex);
      }
    }
    return repositories;
  }

  protected void createWebHooksForRepository(
      RepositoryData repo, OpenProjectData project, String componentType) {

    if (noWebhookComponents.contains(componentType)) {
      logger.info(
          "won't create a webhook for repo '{}' as its component type is '{}' which is contained in the webhook proxy blacklist '{}'",
          repo.getName(),
          componentType,
          noWebhookComponents);
      return;
    }

    String webhookProxyHost =
        String.format(
            projectOpenshiftJenkinsWebhookProxyNamePattern,
            project.getProjectKey().toLowerCase(),
            projectOpenshiftBaseDomain);
    String triggerSecret =
        project.getWebhookProxySecret() != null
            ? project.getWebhookProxySecret()
            : projectOpenshiftJenkinsTriggerSecret;
    String webhookProxyUrl = "https://" + webhookProxyHost + "?trigger_secret=" + triggerSecret;
    Webhook webhook = new Webhook();
    webhook.setName("Jenkins");
    webhook.setActive(true);
    webhook.setUrl(webhookProxyUrl);
    webhook.setEvents(webhookEvents);

    String url =
        String.format(
            "%s/%s/repos/%s/webhooks", getAdapterApiUri(), project.getProjectKey(), repo.getSlug());

    try {
      RestClientCall call = httpPost().url(url).body(webhook).returnType(Webhook.class);
      getRestClient().execute(call);
      logger.info("created hook: {}", webhook.getUrl());
    } catch (IOException ex) {
      logger.error("Error in webhook call", ex);
    }
  }

  protected BitbucketProjectData callCreateProjectApi(OpenProjectData project) throws IOException {
    BitbucketProject bbProject = createBitbucketProject(project);

    RestClientCall call =
        httpPost().url(getAdapterApiUri()).body(bbProject).returnType(BitbucketProjectData.class);
    BitbucketProjectData projectData = getRestClient().execute(call);
    if (project.isSpecialPermissionSet()) {
      setProjectPermissions(
          projectData, ID_GROUPS, globalKeyuserRoleName, PROJECT_PERMISSIONS.PROJECT_ADMIN);
      setProjectPermissions(
          projectData,
          ID_GROUPS,
          project.getProjectAdminGroup(),
          PROJECT_PERMISSIONS.PROJECT_ADMIN);
      setProjectPermissions(
          projectData, ID_GROUPS, project.getProjectUserGroup(), PROJECT_PERMISSIONS.PROJECT_WRITE);
      setProjectPermissions(
          projectData,
          ID_GROUPS,
          project.getProjectReadonlyGroup(),
          PROJECT_PERMISSIONS.PROJECT_READ);
    } else {
      setProjectPermissions(
          projectData, ID_GROUPS, defaultUserGroup, PROJECT_PERMISSIONS.PROJECT_WRITE);
      setProjectPermissions(
          projectData, ID_GROUPS, openDevStackUsersGroupName, PROJECT_PERMISSIONS.PROJECT_READ);
    }

    String projectCdUser = technicalUser;

    // proof if CD user is project specific, if so, set read permissions to global read repos
    if (project.getCdUser() != null && !project.getCdUser().trim().isEmpty()) {
      projectCdUser = project.getCdUser();

      String finalCdUser = projectCdUser;

      scmGlobalProperties
          .getReadableRepos()
          .forEach(
              (k, v) -> {
                logger.debug("Set permissions for repos in project {}", k);
                for (String repo : v) {
                  try {
                    logger.debug(
                        "Set permission for repo {} in project {} for user {} ",
                        repo,
                        k,
                        finalCdUser);
                    setRepositoryPermissions(
                        repo, k, ID_USERS, finalCdUser, REPOSITORY_PERMISSIONS.REPO_READ);
                  } catch (IOException e) {
                    logger.error("Unable to set global read permission for cd user", e);
                  }
                }
              });
    }
    // set the technical user in any case
    setProjectPermissions(projectData, ID_USERS, projectCdUser, PROJECT_PERMISSIONS.PROJECT_WRITE);

    return projectData;
  }

  protected RepositoryData callCreateRepoApi(String projectKey, Repository repo)
      throws IOException {
    String path = String.format("%s/%s/repos", getAdapterApiUri(), projectKey);

    RepositoryData data =
        getRestClient().execute(httpPost().url(path).body(repo).returnType(RepositoryData.class));
    if (data == null) {
      throw new IOException(
          String.format(
              "Repo %s, for project %s could not be created"
                  + " - no response from endpoint, please check logs",
              repo.getName(), projectKey));
    }
    setRepositoryAdminPermissions(data, projectKey, ID_GROUPS, repo.getUserGroup());
    setRepositoryWritePermissions(data, projectKey, ID_USERS, technicalUser);
    if (grantRepositoryWriteToAllOpenDevStackUsers) {
      logger.info(
          "Grant write to every member of {} to repository {}",
          openDevStackUsersGroupName,
          data.getSlug());
      setRepositoryPermissions(
          data.getSlug(),
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

    Map<String, String> header = new HashMap<>();
    header.put("Content-Type", "application/json");
    getRestClient()
        .execute(
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
        data.getSlug(), key, userOrGroup, userOrGroupName, REPOSITORY_PERMISSIONS.REPO_ADMIN);
  }

  protected void setRepositoryWritePermissions(
      RepositoryData data, String key, String userOrGroup, String userOrGroupName)
      throws IOException {
    setRepositoryPermissions(
        data.getSlug(), key, userOrGroup, userOrGroupName, REPOSITORY_PERMISSIONS.REPO_WRITE);
  }

  private void setRepositoryPermissions(
      String repo,
      String key,
      String userOrGroup,
      String userOrGroupName,
      REPOSITORY_PERMISSIONS permission)
      throws IOException {
    String basePath = getAdapterApiUri();
    String url = String.format("%s/%s/repos/%s/permissions/%s", basePath, key, repo, userOrGroup);

    getRestClient()
        .execute(
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
    project.setKey(jiraProject.getProjectKey());
    project.setName(jiraProject.getProjectName());
    project.setDescription(
        (jiraProject.getDescription() != null) ? jiraProject.getDescription() : "");
    return project;
  }

  @Override
  public String getAdapterApiUri() {
    return String.format(BITBUCKET_API_PROJECTS_PATTERN, bitbucketUri, bitbucketApiPath);
  }

  public String getAdapterRootApiUri() {
    return String.format("%s%s", bitbucketUri, bitbucketApiPath);
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException("BitbucketAdapter#getProjects");
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
    if (project.getRepositories() == null || project.getRepositories().size() == 0) {
      logger.debug("Project {} not affected from cleanup, no repos", project.getProjectKey());
      return leftovers;
    }

    Set<String> repositoryNames = project.getRepositories().keySet();

    logger.debug("Cleanup of {} scm repositories", repositoryNames);

    int failedRepoCleanup = 0;

    for (String repoName : repositoryNames) {
      try {
        String repoPath =
            String.format("%s/%s/repos/%s", getAdapterApiUri(), project.getProjectKey(), repoName);
        getRestClient().execute(httpDelete().url(repoPath).returnType(null));
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

    logger.debug("Starting scm project cleanup with url {}", project.getScmvcsUrl());

    String projectPath = String.format("%s/%s", getAdapterApiUri(), project.getProjectKey());

    try {
      getRestClient().execute(httpDelete().url(projectPath).returnType(null));
    } catch (Exception eProjectDelete) {
      logger.debug(
          "Could not remove project {}, error {}",
          project.getProjectKey(),
          eProjectDelete.getMessage());
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.SCM_PROJECT, 1);
    }

    project.setScmvcsUrl(null);

    logger.debug(
        "Cleanup done - status: {} components are left ..", leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }

  public String getTechnicalUser() {
    return technicalUser;
  }
}
