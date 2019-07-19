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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.Context;
import org.opendevstack.provision.model.confluence.JiraServer;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.model.confluence.SpaceData;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;

/**
 * Service to interact with and add Spaces
 *
 * @author Brokmeier, Pascal
 */
@Service
public class ConfluenceAdapter implements ICollaborationAdapter {
  private static final Logger logger = LoggerFactory.getLogger(ConfluenceAdapter.class);

  @Value("${confluence.api.path}")
  private String confluenceApiPath;

  @Value("${confluence.json.rpc.api.path}")
  private String confluenceLegacyApiPath;

  @Value("${confluence.uri}")
  private String confluenceUri;

  @Value("${jira.uri}")
  private String jiraUri;

  @Autowired
  RestClient client;

  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;

  private static final String SPACE_PATTERN = "%s%s/create-dialog/1.0/space-blueprint/create-space";
  private static final String BLUEPRINT_PATTERN =
      "%s%s/create-dialog/1.0/space-blueprint/dialog/web-items";
  private static final String JIRA_SERVER = "%s%s/jiraanywhere/1.0/servers";

  private static final String SPACE_GROUP = "SPACE_GROUP";

  @Value("${confluence.permission.filepattern}")
  private String confluencePermissionFilePattern;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Autowired
  ConfigurableEnvironment environment;

  @Autowired
  List<String> projectTemplateKeyNames;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  public String createCollaborationSpaceForODSProject(OpenProjectData project) throws IOException {
    SpaceData space = callCreateSpaceApi(createSpaceData(project));
    String spaceUrl = space.getUrl();

    if (project.specialPermissionSet) {
      try {
        updateSpacePermissions(project);
      } catch (Exception createPermissions) {
        // continue - we are ok if permissions fail, because the admin has access, and
        // create the set
        logger.error("Could not create project: " + project.projectKey + "\n Exception: "
            + createPermissions.getMessage());
      }
    }

    return spaceUrl;
  }

  protected SpaceData callCreateSpaceApi(Space space) throws IOException {
    String path = String.format(SPACE_PATTERN, confluenceUri, confluenceApiPath);
    return client.callHttp(path, space, false, RestClient.HTTP_VERB.POST, SpaceData.class);
  }

  Space createSpaceData(OpenProjectData project) throws IOException {
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
    String url = String.format(JIRA_SERVER, confluenceUri, confluenceApiPath);
    List<Object> server = getSpaceTemplateList(url, new TypeReference<List<JiraServer>>() {});
    for (Object obj : server) {
      logger.debug("Server: {}", obj);
      JiraServer jiraServer = (JiraServer) obj;
      if (jiraServer.getUrl().equals(jiraUri)) {
        jiraServerId = jiraServer.getId();
      }
    }
    return jiraServerId;
  }

  private String getBluePrintId(String projectTypeKey) throws IOException {
    String bluePrintId = null;
    String url = String.format(BLUEPRINT_PATTERN, confluenceUri, confluenceApiPath);
    List<Object> blueprints = getSpaceTemplateList(url, new TypeReference<List<Blueprint>>() {});

    OpenProjectData project = new OpenProjectData();
    project.projectType = projectTypeKey;

    String template = retrieveInternalProjectTypeAndTemplateFromProjectType(project)
        .get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY);

    for (Object obj : blueprints) {
      Blueprint blueprint = (Blueprint) obj;
      logger.debug("Blueprint: {} searchKey: {}", blueprint.getBlueprintModuleCompleteKey(),
          template);
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

  List<Object> getSpaceTemplateList(String url, TypeReference reference) throws IOException {
    client.getSessionId(confluenceUri);

    return (List<Object>) client.callHttpTypeRef(url, null, false, RestClient.HTTP_VERB.GET,
        reference);
  }

  int updateSpacePermissions(OpenProjectData data) throws IOException {
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
        permissionset = permissionset.replace("SPACE_NAME", data.projectKey);

        if (permissionFilename.contains("adminGroup")) {
          permissionset = permissionset.replace(SPACE_GROUP, data.projectAdminGroup);
        } else if (permissionFilename.contains("userGroup")) {
          permissionset = permissionset.replace(SPACE_GROUP, data.projectUserGroup);
        } else if (permissionFilename.contains("readonlyGroup")) {
          permissionset = permissionset.replace(SPACE_GROUP, data.projectReadonlyGroup);
        } else if (permissionFilename.contains("keyuserGroup")) {
          permissionset = permissionset.replace(SPACE_GROUP, globalKeyuserRoleName);
        }

        String path =
            String.format("%s%s/addPermissionsToSpace", confluenceUri, confluenceLegacyApiPath);

        client.callHttp(path, permissionset, false, RestClient.HTTP_VERB.POST, String.class);

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

    template.put(PROJECT_TEMPLATE.TEMPLATE_KEY,
        (projectTypeKey != null && !projectTypeKey.equals(defaultProjectKey)
            && environment.containsProperty(confluencetemplateKeyPrefix + projectTypeKey)
            && projectTemplateKeyNames.contains(projectTypeKey))
                ? environment.getProperty(confluencetemplateKeyPrefix + projectTypeKey)
                : confluenceBlueprintKey);

    return template;
  }

  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException();
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(LIFECYCLE_STAGE stage,
      OpenProjectData project) {
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

    logger.debug("Cleaning up collaboration space: {} with url {}", project.projectKey,
        project.collaborationSpaceUrl);

    String confluenceProjectPath =
        String.format("%s/api/space/%s", getAdapterApiUri(), project.projectKey);

    try {
      client.callHttp(confluenceProjectPath, null, true, HTTP_VERB.DELETE, null);

      project.collaborationSpaceUrl = null;
    } catch (Exception cex) {
      logger.error(
          String.format("Could not clean up project" + " {} error: {}", project.projectKey, cex));
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.COLLABORATION_SPACE, 1);
    }

    logger.debug("Cleanup done - status: {} components are left ..",
        leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }

}
