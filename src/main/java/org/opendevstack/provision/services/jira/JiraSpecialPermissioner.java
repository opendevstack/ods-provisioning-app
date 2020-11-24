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
package org.opendevstack.provision.services.jira;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendevstack.provision.config.JiraProjectTemplateProperties;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jira.Permission;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;


@Component
public class JiraSpecialPermissioner {

  private static final Logger logger =
      LoggerFactory.getLogger(WebhookProxyJiraPropertyUpdater.class);

  @Autowired private JiraProjectTemplateProperties jiraProjectTemplateProperties;

  @Value("${jira.api.path}")
  private String jiraApiPath;

  @Value("${jira.uri}")
  private String jiraUri;

  @Value("${jira.permission.filepattern}")
  private String jiraPermissionFilePattern;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Value("${jira.reuse-permission-scheme.enabled:true}")
  private boolean reusePermissionScheme;

  /**
   * If project type was configured with permission scheme id then use this configuration otherwise
   * use project template type configuration If both are missing create a new default permission
   * scheme
   *
   * @param jiraRestService
   * @param project
   * @throws IOException
   */
  public void setupProjectSpecialPermissions(
      JiraRestService jiraRestService, OpenProjectData project) throws IOException {

    if (reusePermissionScheme && project.getSpecialPermissionSchemeId() != null) {

      updateProjectPermissionSchemeAndRoles(jiraRestService, project);

    } else if (reusePermissionScheme
        && jiraProjectTemplateProperties
            .getProjectTemplates()
            .containsKey(project.getProjectType())) {

      JiraProjectTemplate projectTemplate =
          jiraProjectTemplateProperties.getProjectTemplates().get(project.getProjectType());

      updateProjectPermissionSchemeAndRoles(jiraRestService, project, projectTemplate);

    } else {
      logger.info(
          "Creating a new permission scheme because neither project defines a permission scheme id nor an id is configured for the project template type! [projectKEy={}, projectTemplateType={}]",
          project.getProjectKey(),
          project.getProjectType());
      createNewSpecialPermissionsAndUpdateProject(jiraRestService, project);
    }
  }

  private void updateProjectPermissionSchemeAndRoles(
      JiraRestService jiraRestService, OpenProjectData project) throws IOException {

    String projectKey = project.getProjectKey();

    logger.info(
        "Using special permission provided from project definition [projectKey={}, permissionSchemeId={}]",
        projectKey,
        project.getSpecialPermissionSchemeId());

    updateProjectPermissionSchemeId(
        jiraRestService, projectKey, project.getSpecialPermissionSchemeId().toString());

    assignGroupsToProjectRoles(
        jiraRestService, projectKey, createGroupsToProjectRolesMappings(project));
  }

  private void updateProjectPermissionSchemeAndRoles(
      JiraRestService jiraRestService, OpenProjectData project, JiraProjectTemplate projectTemplate)
      throws IOException {

    String projectKey = project.getProjectKey();

    logger.info(
        "Using special permission from project template type configuration [projectKey={}, permissionSchemeId={}, projectTemplate={}]",
        projectKey,
        projectTemplate.getPermissionSchemeId(),
        projectTemplate.getName());

    updateProjectPermissionSchemeId(
        jiraRestService, projectKey, projectTemplate.getPermissionSchemeId().toString());

    assignGroupsToProjectRoles(
        jiraRestService, projectKey, createGroupsToProjectRolesMappings(project, projectTemplate));
  }

  /**
   * Create permission set for jira project
   *
   * @param project the project
   * @return the number of created permission sets
   */
  public void createNewSpecialPermissionsAndUpdateProject(
      JiraRestService jiraRestService, OpenProjectData project) {

    PathMatchingResourcePatternResolver pmrl =
        new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());
    try {
      Resource[] permissionFiles = pmrl.getResources(jiraPermissionFilePattern);

      logger.debug("Found permissionsets: {}", permissionFiles.length);

      for (Resource permissionFile : permissionFiles) {
        PermissionScheme singleScheme =
            new ObjectMapper().readValue(permissionFile.getInputStream(), PermissionScheme.class);

        String permissionSchemeName = project.projectKey + " PERMISSION SCHEME";

        singleScheme.setName(permissionSchemeName);

        String description = project.description;
        if (description != null && description.length() > 0) {
          singleScheme.setDescription(description);
        } else {
          singleScheme.setDescription(permissionSchemeName);
        }

        // replace group with real group
        for (Permission permission : singleScheme.getPermissions()) {
          String group = permission.getHolder().getParameter();

          if ("adminGroup".equals(group)) {
            permission.getHolder().setParameter(project.projectAdminGroup);
          } else if ("userGroup".equals(group)) {
            permission.getHolder().setParameter(project.projectUserGroup);
          } else if ("readonlyGroup".equals(group)) {
            permission.getHolder().setParameter(project.projectReadonlyGroup);
          } else if ("keyuserGroup".equals(group)) {
            permission.getHolder().setParameter(globalKeyuserRoleName);
          }
        }
        logger.debug(
            "Update permissionScheme {} location: {}",
            permissionSchemeName,
            permissionFile.getFilename());

        String path =
            String.format(JiraRestApi.JIRA_API_PERMISSION_SCHEME_PATTERN, jiraUri, jiraApiPath);
        RestClientCall call =
            jiraRestService
                .httpPost()
                .url(path)
                .body(singleScheme)
                .returnTypeReference(new TypeReference<PermissionScheme>() {});

        PermissionScheme newPermissionScheme = jiraRestService.getRestClient().execute(call);

        updateProjectPermissionSchemeId(
            jiraRestService, project.getProjectKey(), newPermissionScheme.getId());
      }
    } catch (Exception createPermissions) {
      // continue - we are ok if permissions fail, because the admin has access, and
      // can create / link the set
      logger.error(
          "Could not update jira project permissionset: {} Exception: {} ",
          project.projectKey,
          createPermissions.getMessage());
    }
    return;
  }

  public void updateProjectPermissionSchemeId(
      JiraRestService jiraRestService, String projectKey, String permissionSchemeId)
      throws IOException {
    String url =
        String.format(
            JiraRestApi.JIRA_API_PROJECT_PERMISSION_SCHEME_PATTERN,
            jiraUri,
            jiraApiPath,
            projectKey);
    PermissionScheme scheme = new PermissionScheme();
    scheme.setId(permissionSchemeId);
    logger.info(
        "Sending Jira API request to assign permission scheme id to project! [projectKey={}, permissionSchemeId={}]",
        projectKey,
        permissionSchemeId);
    jiraRestService
        .getRestClient()
        .execute(jiraRestService.httpPut().body(scheme).url(url).returnType(String.class));
  }

  private List<Pair> createGroupsToProjectRolesMappings(
      OpenProjectData project, JiraProjectTemplate projectTemplate) {

    List mappings = new ArrayList<Pair>();

    JiraProjectTemplateRoleMapping roleMapping = projectTemplate.getRoleMapping();

    if (null != project.getProjectAdminGroup()
        && null != roleMapping.getProjectRoleForAdminGroup()) {
      mappings.add(
          new Pair(
              roleMapping.getProjectRoleForAdminGroup(),
              new ActorGroupPayload(project.getProjectAdminGroup())));
    }

    if (null != project.getProjectUserGroup() && null != roleMapping.getProjectRoleForUserGroup()) {
      mappings.add(
          new Pair(
              roleMapping.getProjectRoleForUserGroup(),
              new ActorGroupPayload(project.getProjectUserGroup())));
    }

    if (null != project.getProjectReadonlyGroup()
        && null != roleMapping.getProjectRoleForReadonlyGroup()) {
      mappings.add(
          new Pair(
              roleMapping.getProjectRoleForReadonlyGroup(),
              new ActorGroupPayload(project.getProjectReadonlyGroup())));
    }

    logger.debug("Created group to project role mappings [mappings={}]", mappings);

    return mappings;
  }

  private List<Pair> createGroupsToProjectRolesMappings(OpenProjectData project) {

    List mappings = new ArrayList<Pair>();

    if (null != project.getProjectAdminGroup() && null != project.getProjectRoleForAdminGroup()) {
      mappings.add(
          new Pair(
              project.getProjectRoleForAdminGroup(),
              new ActorGroupPayload(project.getProjectAdminGroup())));
    }

    if (null != project.getProjectUserGroup() && null != project.getProjectRoleForUserGroup()) {
      mappings.add(
          new Pair(
              project.getProjectRoleForUserGroup(),
              new ActorGroupPayload(project.getProjectUserGroup())));
    }

    if (null != project.getProjectReadonlyGroup()
        && null != project.getProjectRoleForReadonlyGroup()) {
      mappings.add(
          new Pair(
              project.getProjectRoleForReadonlyGroup(),
              new ActorGroupPayload(project.getProjectReadonlyGroup())));
    }

    logger.debug("Created group to project role mappings [mappings={}]", mappings);

    return mappings;
  }

  private void assignGroupsToProjectRoles(
      JiraRestService jiraRestService, String projectKey, List<Pair> roleMapping) {

    roleMapping.forEach(
        (pair) -> {
          String url =
              String.format(
                  JiraRestApi.JIRA_API_PROJECT_ROLE_PATTERN,
                  jiraUri,
                  jiraApiPath,
                  projectKey,
                  pair.roleId);
          try {
            jiraRestService
                .getRestClient()
                .execute(
                    jiraRestService
                        .httpPost()
                        .body(pair.actorGroupPayload)
                        .url(url)
                        .returnType(null));
          } catch (IOException e) {
            if (e.getMessage().contains("is already a member of the project role")) {
              logger.warn(
                  "Ignoring error code as group is already member of project role! [projectKey={}, errorMessage={}]",
                  projectKey,
                  e.getMessage());
            } else {
              throw new RuntimeException(e);
            }
          }
        });
  }

  public static class Pair {

    private final String roleId;
    private final ActorGroupPayload actorGroupPayload;

    Pair(String roleId, ActorGroupPayload actorGroupPayload) {
      this.roleId = roleId;
      this.actorGroupPayload = actorGroupPayload;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Pair pair = (Pair) o;

      if (roleId != null ? !roleId.equals(pair.roleId) : pair.roleId != null) return false;
      return actorGroupPayload != null
          ? actorGroupPayload.equals(pair.actorGroupPayload)
          : pair.actorGroupPayload == null;
    }

    @Override
    public int hashCode() {
      int result = roleId != null ? roleId.hashCode() : 0;
      result = 31 * result + (actorGroupPayload != null ? actorGroupPayload.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "Pair{"
          + "roleId='"
          + roleId
          + '\''
          + ", actorGroupPayload="
          + actorGroupPayload
          + '}';
    }
  }
}
