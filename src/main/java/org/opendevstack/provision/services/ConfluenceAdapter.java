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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.NotImplementedException;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.confluence.*;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Service to interact with and add Spaces */
@Service
@ConditionalOnProperty(
    name = "adapters.confluence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ConfluenceAdapter extends BaseServiceAdapter implements ICollaborationAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ConfluenceAdapter.class);

  public static final String ADAPTER_NAME = "confluence";

  public static final String CONFLUENCE_API_SPACE = "api/space";
  public static final String CONFLUENCE_API_USER = "api/user";
  public static final String CONFLUENCE_API_GROUP = "api/group";
  public static final String CONFLUENCE_API_CREATE_DIALOG_SPACE_BLUEPRINT =
      "create-dialog/1.0/space-blueprint";

  public static final String BASE_PATTERN = "%s%s/";
  public static final String CONFLUENCE_API_GROUP_PATTERN =
      BASE_PATTERN + CONFLUENCE_API_GROUP + "/%s";
  public static final String CONFLUENCE_API_USER_PATTERN = BASE_PATTERN + CONFLUENCE_API_USER;
  public static final String CONFLUENCE_API_CREATE_SPACE_PATTERN =
      BASE_PATTERN + CONFLUENCE_API_CREATE_DIALOG_SPACE_BLUEPRINT + "/create-space";
  public static final String CONFLUENCE_API_BLUEPRINT_PATTERN =
      BASE_PATTERN + CONFLUENCE_API_CREATE_DIALOG_SPACE_BLUEPRINT + "/dialog/web-items";
  public static final String CONFLUENCE_API_ADD_PERMISSIONS_TO_SPACE_PATTERN =
      BASE_PATTERN + "addPermissionsToSpace";
  public static final String CONFLUENCE_API_JIRA_SERVER_PATTERN =
      BASE_PATTERN + "jiraanywhere/1.0/servers";

  public static final String CONFLUENCE_API_SPACE_PATTERN =
      BASE_PATTERN + CONFLUENCE_API_SPACE + "/%s";

  public static final String SPACE_GROUP_KEY = "SPACE_GROUP";
  public static final String SPACE_NAME_KEY = "SPACE_NAME";

  public static final String ADMIN_GROUP = "adminGroup";
  public static final String USER_GROUP = "userGroup";
  public static final String READONLY_GROUP = "readonlyGroup";
  public static final String KEYUSER_GROUP = "keyuserGroup";

  public static final String USERNAME_PARAM = "username";

  @Value("${confluence.api.path}")
  private String confluenceApiPath;

  @Value("${confluence.json.rpc.api.path}")
  private String confluenceLegacyApiPath;

  @Value("${confluence.uri}")
  private String confluenceUri;

  @Value("${jira.uri}")
  private String jiraUri;

  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;

  @Value("${confluence.permission.filepattern}")
  private String confluencePermissionFilePattern;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Autowired ConfigurableEnvironment environment;

  @Qualifier("projectTemplateKeyNames")
  @Autowired
  List<String> projectTemplateKeyNames;

  public ConfluenceAdapter() {
    super(ADAPTER_NAME);
  }

  public String createCollaborationSpaceForODSProject(OpenProjectData project) throws IOException {
    SpaceData space = callCreateSpaceApi(createSpaceData(project));
    String spaceUrl = space.getUrl();

    if (project.specialPermissionSet) {
      try {
        updateSpacePermissions(project);
      } catch (Exception createPermissions) {
        // continue - we are ok if permissions fail, because the admin has access, and
        // create the set
        logger.error(
            "Could not create project: "
                + project.projectKey
                + "\n Exception: "
                + createPermissions.getMessage());
      }
    }

    return spaceUrl;
  }

  public SpaceData callCreateSpaceApi(Space space) throws IOException {
    String path =
        String.format(CONFLUENCE_API_CREATE_SPACE_PATTERN, confluenceUri, confluenceApiPath);
    return getRestClient()
        .execute(
            httpPost()
                .url(path)
                .body(space)
                .returnTypeReference(new TypeReference<SpaceData>() {}));
  }

  public Space createSpaceData(OpenProjectData project) throws IOException {
    String confluenceBlueprintId = getBluePrintId(project.projectType);
    String jiraServerId = getJiraServerId();

    Space space = new Space();
    space.setSpaceBlueprintId(confluenceBlueprintId);
    space.setName(project.projectName);
    space.setSpaceKey(project.projectKey);
    space.setDescription(project.description);

    Context context = new Context();
    context.setName(project.projectName);
    context.setSpaceKey(project.projectKey);
    context.setAtlToken("undefined");
    context.setNoPageTitlePrefix("true");
    context.setJiraServer(jiraServerId);
    context.setJiraServerId(jiraServerId);
    context.setProjectKey(project.projectKey);
    context.setProjectName(project.projectName);
    context.setDescription(project.description);
    space.setContext(context);
    return space;
  }

  private String getJiraServerId() throws IOException {
    String jiraServerId = null;
    String url =
        String.format(CONFLUENCE_API_JIRA_SERVER_PATTERN, confluenceUri, confluenceApiPath);
    List<JiraServer> servers = getSpaceTemplateList(url, new TypeReference<>() {});
    for (var server : servers) {
      logger.debug("Server: {}", server);
      if (server.getUrl().equals(jiraUri)) {
        jiraServerId = server.getId();
      }
    }
    return jiraServerId;
  }

  private String getBluePrintId(String projectTypeKey) throws IOException {
    String bluePrintId = null;
    String url = String.format(CONFLUENCE_API_BLUEPRINT_PATTERN, confluenceUri, confluenceApiPath);
    List<Blueprint> blueprints = getSpaceTemplateList(url, new TypeReference<>() {});
    OpenProjectData project = new OpenProjectData();
    project.projectType = projectTypeKey;
    String template =
        retrieveInternalProjectTypeAndTemplateFromProjectType(project)
            .get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY);

    for (var blueprint : blueprints) {
      logger.debug(
          "Blueprint: {} searchKey: {}", blueprint.getBlueprintModuleCompleteKey(), template);
      if (blueprint.getBlueprintModuleCompleteKey().equals(template)) {
        bluePrintId = blueprint.getContentBlueprintId();
        break;
      }
    }
    if (bluePrintId == null) {
      // default
      return getBluePrintId(null);
    }
    return bluePrintId;
  }

  public <T> T getSpaceTemplateList(String url, TypeReference<T> reference) throws IOException {
    return getRestClient().execute(httpGet().url(url).returnTypeReference(reference));
  }

  public int updateSpacePermissions(OpenProjectData data) throws IOException {
    PathMatchingResourcePatternResolver pmrl =
        new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());

    Resource[] permissionFiles = pmrl.getResources(confluencePermissionFilePattern);

    int updatedPermissions = 0;

    logger.debug("Found permission sets: {}", permissionFiles.length);

    for (Resource permissionFile : permissionFiles) {
      String permissionFilename = permissionFile.getFilename();

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(permissionFile.getInputStream()))) {
        String permissionset;

        // we know it's a singular pseudo json line
        permissionset = reader.readLine();
        permissionset = permissionset.replace(SPACE_NAME_KEY, data.projectKey);

        if (permissionFilename.contains(ADMIN_GROUP)) {
          permissionset = permissionset.replace(SPACE_GROUP_KEY, data.projectAdminGroup);
        } else if (permissionFilename.contains(USER_GROUP)) {
          permissionset = permissionset.replace(SPACE_GROUP_KEY, data.projectUserGroup);
        } else if (permissionFilename.contains(READONLY_GROUP)) {
          permissionset = permissionset.replace(SPACE_GROUP_KEY, data.projectReadonlyGroup);
        } else if (permissionFilename.contains(KEYUSER_GROUP)) {
          permissionset = permissionset.replace(SPACE_GROUP_KEY, globalKeyuserRoleName);
        }

        String path =
            String.format(
                CONFLUENCE_API_ADD_PERMISSIONS_TO_SPACE_PATTERN,
                confluenceUri,
                confluenceLegacyApiPath);

        getRestClient().execute(httpPost().url(path).body(permissionset).returnType(String.class));

        updatedPermissions++;
      }
    }
    return updatedPermissions;
  }

  public String getAdapterApiUri() {
    return confluenceUri + confluenceApiPath;
  }

  @Override
  public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
      OpenProjectData project) {
    Preconditions.checkNotNull(project, "project cannot be null");

    String projectTypeKey = project.projectType;

    String confluencetemplateKeyPrefix = "confluence.blueprint.key.";

    Map<PROJECT_TEMPLATE, String> template = new HashMap<>();

    template.put(
        PROJECT_TEMPLATE.TEMPLATE_KEY,
        (projectTypeKey != null
                && !projectTypeKey.equals(defaultProjectKey)
                && environment.containsProperty(confluencetemplateKeyPrefix + projectTypeKey)
                && projectTemplateKeyNames.contains(projectTypeKey))
            ? environment.getProperty(confluencetemplateKeyPrefix + projectTypeKey)
            : confluenceBlueprintKey);

    return template;
  }

  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException("ConfluenceAdapter#getProjects");
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    if (stage.equals(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION)
        || (!project.bugtrackerSpace && project.collaborationSpaceUrl == null)) {
      logger.debug("Project {} not affected from cleanup", project.projectKey);
      return leftovers;
    }

    if (project.collaborationSpaceUrl == null) {
      logger.debug("Project {} not affected from cleanup", project.projectKey);
      return new HashMap<>();
    }

    logger.debug(
        "Cleaning up collaboration space: {} with url {}",
        project.projectKey,
        project.collaborationSpaceUrl);

    String confluenceProjectPath =
        String.format("%s/api/space/%s", getAdapterApiUri(), project.projectKey);

    try {
      getRestClient().execute(httpDelete().body("").url(confluenceProjectPath));

      project.collaborationSpaceUrl = null;
    } catch (Exception cex) {
      logger.error("Could not clean up project {} -  error: {}", project.projectKey, cex);
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.COLLABORATION_SPACE, 1);
    }

    logger.debug(
        "Cleanup done - status: {} components are left ..", leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }

  /**
   * This method verifies if the required groups exists in Confluence
   *
   * <p>Note: currently (Confluence server version 6.15) is not possible to verify by rest api call
   * if a user has the 'create project permission'.
   */
  @Override
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {

    try {
      Assert.notNull(newProject, "Parameter 'newProject' is null!");
      Assert.notNull(
          newProject.projectKey, "Properties 'projectKey' of parameter 'newProject' is null!");

      logger.info("checking create project preconditions for project '{}'!", newProject.projectKey);

      List<CheckPreconditionFailure> preconditionFailures =
          createProjectKeyExistsCheck(newProject.getProjectKey())
              .andThen(createRequiredGroupExistsCheck(newProject))
              .andThen(createCheckUser(newProject))
              .apply(new ArrayList<>());

      logger.info(
          "done with check create project preconditions for project '{}'!", newProject.projectKey);

      return Collections.unmodifiableList(preconditionFailures);

    } catch (AdapterException e) {
      throw new CreateProjectPreconditionException(ADAPTER_NAME, newProject.projectKey, e);
    } catch (Exception e) {
      String message =
          String.format(
              "Unexpected error when checking precondition for creation of project '%s'",
              newProject.projectKey);
      logger.error(message, e);
      throw new CreateProjectPreconditionException(ADAPTER_NAME, newProject.projectKey, message);
    }
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createRequiredGroupExistsCheck(OpenProjectData newProject) {
    return preconditionFailures -> {

      // get groups required based from permission template file
      Set<String> groups = null;
      try {
        groups = groupsRequired(newProject);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      groups.forEach(
          group -> {
            try {
              if (!checkGroupExists(group)) {
                String message =
                    String.format("Group '%s' does not exists in %s!", group, ADAPTER_NAME);
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

  private boolean checkGroupExists(String group) throws IOException {

    logger.info("checking if group '{}' exists!", group);

    String url =
        String.format(CONFLUENCE_API_GROUP_PATTERN, confluenceUri, confluenceApiPath, group);

    String response = null;
    try {
      response = getRestClient().execute(httpGet().url(url).returnType(String.class));
      Assert.notNull(response, "Response is null for '" + group + "'");
    } catch (HttpException e) {
      if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
        logger.debug("Group '{}' was not found in {}!", group, ADAPTER_NAME, e);
        return false;
      } else {
        logger.warn("Unexpected method trying to get group '{}'!", group, e);
        throw e;
      }
    }

    JsonNode json = new ObjectMapper().readTree(response);

    JsonNode haveGroup = json.at("/name");

    if (MissingNode.class.isInstance(haveGroup)) {
      logger.warn("Missing node for '{}'!", json);
      return false;
    }

    return group.equalsIgnoreCase(haveGroup.asText());
  }

  private Set<String> groupsRequired(OpenProjectData data) throws IOException {

    PathMatchingResourcePatternResolver pmrl =
        new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());

    Resource[] permissionFiles = pmrl.getResources(confluencePermissionFilePattern);

    Set<String> groupsRequired = new HashSet<>();

    BiConsumer<Set<String>, String> addIfNotEmpty =
        (set, value) -> {
          if (null != value && !value.isEmpty()) {
            set.add(value);
          }
        };

    for (Resource permissionFile : permissionFiles) {

      String permissionFilename = permissionFile.getFilename();

      if (permissionFilename.contains(ADMIN_GROUP)) {
        addIfNotEmpty.accept(groupsRequired, data.projectAdminGroup);
      } else if (permissionFilename.contains(USER_GROUP)) {
        addIfNotEmpty.accept(groupsRequired, data.projectUserGroup);
      } else if (permissionFilename.contains(READONLY_GROUP)) {
        addIfNotEmpty.accept(groupsRequired, data.projectReadonlyGroup);
      } else if (permissionFilename.contains(KEYUSER_GROUP)) {
        addIfNotEmpty.accept(groupsRequired, globalKeyuserRoleName);
      }
    }

    return Collections.unmodifiableSet(groupsRequired);
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> createCheckUser(
      OpenProjectData project) {
    return preconditionFailures -> {
      Set<String> usersToCheck = new HashSet<>();

      // 1: user that makes the api call user
      usersToCheck.add(getUserName());

      // 2: special perm set admin user
      if (project.bugtrackerSpace && project.specialPermissionSet) {
        if (project.projectAdminUser != null && !project.projectAdminUser.isEmpty()) {
          usersToCheck.add(project.projectAdminUser);
        }
      }

      usersToCheck.forEach(
          username -> {
            if (!checkUserExists(username)) {
              String message =
                  String.format("User '%s' does not exists in %s!", username, ADAPTER_NAME);
              preconditionFailures.add(CheckPreconditionFailure.getUnexistantUserInstance(message));
            }
          });

      return preconditionFailures;
    };
  }

  private boolean checkUserExists(String username) {

    Map<String, String> params = new HashMap<>();
    params.put(USERNAME_PARAM, username);

    String url = String.format(CONFLUENCE_API_USER_PATTERN, confluenceUri, confluenceApiPath);
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

      JsonNode usernameNode = json.findPath("username");

      if (usernameNode == null || MissingNode.class.isInstance(usernameNode)) {
        logger.warn("Missing node for '{}'!", json);
        return false;
      }

      return username.equalsIgnoreCase(usernameNode.asText());

    } catch (IOException e) {
      throw new AdapterException(e);
    }
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createProjectKeyExistsCheck(String projectKey) {
    return preconditionFailures -> {
      if (checkProjectExists(projectKey)) {
        String message =
            String.format("Project/Space '%s' already exists in '%s'!", projectKey, ADAPTER_NAME);
        preconditionFailures.add(CheckPreconditionFailure.getProjectExistsInstance(message));
      }

      return preconditionFailures;
    };
  }

  private boolean checkProjectExists(String projectKey) {

    try {
      String response = null;

      try {
        String url =
            String.format(
                CONFLUENCE_API_SPACE_PATTERN, confluenceUri, confluenceApiPath, projectKey);

        response = getRestClient().execute(httpGet().url(url).returnType(String.class), true);
        Assert.notNull(response, "Response is null for '" + projectKey + "'");
      } catch (HttpException e) {
        if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
          logger.debug(
              "Could not find space '{}' in '{}'", projectKey, ADAPTER_NAME, e.getMessage());
          return false;
        } else {
          logger.warn(
              "Unexpected method trying to find space '{}' in '{}'!", projectKey, ADAPTER_NAME, e);
          throw e;
        }
      }

      JsonNode json = new ObjectMapper().readTree(response);

      JsonNode key = json.findPath("key");

      if (key == null || MissingNode.class.isInstance(key)) {
        logger.warn("Missing node for '{}'!", json);
        return false;
      }

      return projectKey.equalsIgnoreCase(key.asText());

    } catch (IOException e) {
      throw new AdapterException(e);
    }
  }
}
