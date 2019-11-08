package org.opendevstack.provision.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jira.Component;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.jira.Permission;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.model.jira.PermissionSchemeResponse;
import org.opendevstack.provision.model.jira.Shortcut;
import org.opendevstack.provision.util.exception.HttpException;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * created by: OPITZ CONSULTING Deutschland GmbH
 *
 * @author Brokmeier, Pascal
 *     <p>To communicate with Jira, we use the Jira REST API
 *     https://developer.atlassian.com/jiradev/jira-apis/jira-rest-apis To ease the burden of
 *     working with a REST API, there is a jira restClient
 *     https://ecosystem.atlassian.net/wiki/display/JRJC/
 */
@Service
public class JiraAdapter extends BaseServiceAdapter implements IBugtrackerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(JiraAdapter.class);

  @Value("${jira.api.path}")
  private String jiraApiPath;

  @Value("${jira.uri}")
  private String jiraUri;

  @Value("${jira.permission.filepattern}")
  private String jiraPermissionFilePattern;

  @Value("${jira.specialpermissionschema.enabled:true}")
  private boolean specialPermissionSchemeEnabled;

  public static final String JIRA_TEMPLATE_KEY_PREFIX = "jira.project.template.key.";
  public static final String JIRA_TEMPLATE_TYPE_PREFIX = "jira.project.template.type.";

  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;

  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @Value("${jira.project.notification.scheme.id:10000}")
  String jiraNotificationSchemeId;

  @Value("${jira.create.components:true}")
  boolean createJiraComponents;

  // Pattern to use for project with id
  private static final String URL_PATTERN = "%s%s/project/%s";

  @Autowired IODSAuthnzAdapter manager;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Autowired ConfigurableEnvironment environment;

  @Autowired List<String> projectTemplateKeyNames;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  public JiraAdapter() {
    super("jira");
  }

  private LeanJiraProject createProjectInJira(OpenProjectData project, FullJiraProject toBeCreated)
      throws IOException {
    LeanJiraProject created;
    try {
      created = this.callJiraCreateProjectApi(toBeCreated);
    } catch (HttpException jiracreateException) {
      logger.debug(
          "error creating project with template {}: {}",
          toBeCreated.projectTemplateKey,
          jiracreateException.getMessage());
      if (jiracreateException.getResponseCode() == 400) {
        logger.info(
            "Template {} did not work, falling back to default {}",
            toBeCreated.projectTemplateKey,
            jiraTemplateKey);
        toBeCreated.projectTypeKey = jiraTemplateType;
        toBeCreated.projectTemplateKey = jiraTemplateKey;
        created = this.callJiraCreateProjectApi(toBeCreated);
        project.projectType = defaultProjectKey;
      } else {
        throw jiracreateException;
      }
    }
    return created;
  }

  protected LeanJiraProject callJiraCreateProjectApi(FullJiraProject jiraProject)
      throws IOException {
    String path = String.format("%s%s/project", jiraUri, jiraApiPath);

    RestClientCall clientCall =
        httpPost().url(path).body(jiraProject).returnType(LeanJiraProject.class);
    LeanJiraProject created = restClient.execute(clientCall);

    return created;
  }

  /**
   * Create permission set for jira project
   *
   * @param project the project
   * @return the number of created permission sets
   */
  protected int createSpecialPermissions(OpenProjectData project) {
    if (!isSpecialPermissionSchemeEnabled()) {
      logger.info(
          "Do not create special permission set for project {}, "
              + "since property jira.specialpermissionschema.enabled=false",
          project.projectKey);
      return 0;
    }
    PathMatchingResourcePatternResolver pmrl =
        new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());
    int updatedPermissions = 0;
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

        String path = String.format("%s%s/permissionscheme", jiraUri, jiraApiPath);
        RestClientCall call =
            httpPost()
                .url(path)
                .body(singleScheme)
                .returnTypeReference(new TypeReference<PermissionScheme>() {});
        singleScheme = restClient.execute(call);

        // update jira project
        path =
            String.format(
                "%s%s/project/%s/permissionscheme", jiraUri, jiraApiPath, project.projectKey);
        PermissionScheme small = new PermissionScheme();
        small.setId(singleScheme.getId());
        // restClient.callHttp(path, small, true, RestClient.HTTP_VERB.PUT, null);
        restClient.execute(httpPut().body(small).url(path).returnType(null));
        updatedPermissions++;
      }
    } catch (Exception createPermissions) {
      // continue - we are ok if permissions fail, because the admin has access, and
      // can create / link the set
      logger.error(
          "Could not update jira project permissionset: {} Exception: {} ",
          project.projectKey,
          createPermissions.getMessage());
    }
    return updatedPermissions;
  }

  protected FullJiraProject buildJiraProjectPojoFromApiProject(OpenProjectData s) {
    String templateKey =
        calculateJiraProjectTypeAndTemplateFromProjectType(
            s, JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey);

    String templateType =
        calculateJiraProjectTypeAndTemplateFromProjectType(
            s, JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType);

    if (jiraTemplateKey.equals(templateKey)) {
      s.projectType = defaultProjectKey;
    }

    logger.debug("Creating project of type: {} for project: {}", templateKey, s.projectKey);

    return new FullJiraProject(
        null,
        s.projectKey,
        s.projectName,
        s.description,
        s.projectAdminUser,
        templateKey,
        templateType,
        jiraNotificationSchemeId);
  }

  public String buildProjectKey(String name) {
    // build key
    String key = name;
    key = key.toUpperCase();
    key = key.replaceAll("\\s+", "");
    key = key.replaceAll("-", "_");
    key = key.length() > 5 ? (key.substring(0, 3) + key.substring(key.length() - 2)) : key;
    return key;
  }

  public boolean projectKeyExists(String key) {
    Preconditions.checkNotNull(key, "Key for keyExists cannot be null");

    Collection<String> projectKeys = getProjectKeys();
    return projectKeys.contains(key);
  }

  // refactor - to only look for the project by key that is to be created!
  public Map<String, String> getProjects(String filter) {
    logger.debug("Getting jira projects with filter {}", filter);
    String url =
        filter == null || filter.trim().length() == 0
            ? String.format("%s%s/project", jiraUri, jiraApiPath)
            : String.format(URL_PATTERN, jiraUri, jiraApiPath, filter);

    try {
      RestClientCall call =
          httpGet().url(url).returnTypeReference(new TypeReference<List<LeanJiraProject>>() {});
      List<LeanJiraProject> projects = restClient.execute(call);
      return convertJiraProjectToKeyMap(projects);
    } catch (IOException e) {
      logger.error("Could not retrieve jira projects", e);
      return convertJiraProjectToKeyMap(null);
    }
  }

  @Override
  public int addShortcutsToProject(OpenProjectData data) {
    if (!data.bugtrackerSpace) {
      return -1;
    }

    String path =
        String.format("%s/rest/projects/1.0/project/%s/shortcut", jiraUri, data.projectKey);

    List<Shortcut> shortcuts = new ArrayList<>();

    int id = 1;
    int createdShortcuts = 0;

    Shortcut shortcutConfluence = new Shortcut();
    shortcutConfluence.setId("" + id++);
    shortcutConfluence.setName("Confluence: " + data.projectKey);
    shortcutConfluence.setUrl(data.bugtrackerUrl);
    shortcutConfluence.setIcon("");
    shortcuts.add(shortcutConfluence);

    if (data.platformRuntime) {
      Shortcut shortcutBB = new Shortcut();
      shortcutBB.setId("" + id++);
      shortcutBB.setName("GIT: " + data.projectKey);
      shortcutBB.setUrl(data.scmvcsUrl);
      shortcutBB.setIcon("");
      shortcuts.add(shortcutBB);

      Shortcut shortcutJenkins = new Shortcut();
      shortcutJenkins.setId("" + id++);
      shortcutJenkins.setName("Jenkins");
      shortcutJenkins.setUrl(data.platformBuildEngineUrl);
      shortcutJenkins.setIcon("");
      shortcuts.add(shortcutJenkins);

      Shortcut shortcutOCDev = new Shortcut();
      shortcutOCDev.setId("" + id++);
      shortcutOCDev.setName("OC Dev " + data.projectKey);
      shortcutOCDev.setUrl(data.platformDevEnvironmentUrl);
      shortcutOCDev.setIcon("");
      shortcuts.add(shortcutOCDev);

      Shortcut shortcutOCTest = new Shortcut();
      shortcutOCTest.setId("" + id);
      shortcutOCTest.setName("OC Test " + data.projectKey);
      shortcutOCTest.setUrl(data.platformTestEnvironmentUrl);
      shortcutOCTest.setIcon("");
      shortcuts.add(shortcutOCTest);
    }

    for (Shortcut shortcut : shortcuts) {
      logger.debug(
          "Attempting to create shortcut {} for: {}", shortcut.getId(), shortcut.getName());
      try {
        // restClient.callHttp(path, shortcut, false, RestClient.HTTP_VERB.POST, Shortcut.class);
        RestClientCall call = httpPost().url(path).body(shortcut).returnType(Shortcut.class);
        restClient.execute(call);
        createdShortcuts++;
      } catch (HttpException httpEx) {
        if (httpEx.getResponseCode() == 401) {
          logger.error(
              "Could not create shortcut for: "
                  + shortcut.getName()
                  + " Error: "
                  + httpEx.getMessage());
          // if you get a 401 here - we can't reach the project, so stop
          break;
        }
      } catch (IOException shortcutEx) {
        logger.error(
            "Could not create shortcut for: "
                + shortcut.getName()
                + " Error: "
                + shortcutEx.getMessage());
      }
    }
    return createdShortcuts;
  }

  public String calculateJiraProjectTypeAndTemplateFromProjectType(
      OpenProjectData project, String templatePrefix, String defaultValue) {
    Preconditions.checkNotNull(templatePrefix, "no template prefix passed");
    Preconditions.checkNotNull(defaultValue, "no defaultValue passed");
    /*
     * if the type can be found in the global definition of types (projectTemplateKeyNames) and is
     * also configured for jira (environment.containsProperty) - take it, if not fall back to
     * default
     */
    return (project.projectType != null
            && environment.containsProperty(templatePrefix + project.projectType)
            && projectTemplateKeyNames.contains(project.projectType))
        ? environment.getProperty(templatePrefix + project.projectType)
        : defaultValue;
  }

  Map<String, String> convertJiraProjectToKeyMap(List<LeanJiraProject> projects) {
    Map<String, String> keyMap = new HashMap<>();
    if (projects == null || projects.size() == 0) {
      return keyMap;
    }

    for (LeanJiraProject project : projects) {
      keyMap.put(project.getKey(), project.getName());
    }
    return keyMap;
  }

  @Override
  public String getAdapterApiUri() {
    return jiraUri + jiraApiPath;
  }

  @Override
  public OpenProjectData createBugtrackerProjectForODSProject(OpenProjectData project)
      throws IOException {
    try {
      logger.debug("Creating new jira project");

      Preconditions.checkNotNull(project.projectKey);
      Preconditions.checkNotNull(project.projectName);

      if (!(project.specialPermissionSet && isSpecialPermissionSchemeEnabled())
          || project.projectAdminUser == null
          || project.projectAdminUser.trim().length() == 0) {
        project.projectAdminUser = manager.getUserName();
      }

      FullJiraProject toBeCreated = this.buildJiraProjectPojoFromApiProject(project);

      LeanJiraProject created = createProjectInJira(project, toBeCreated);

      logger.debug("Created project: {}", created);
      project.bugtrackerUrl = String.format("%s/browse/%s", jiraUri, created.getKey());

      if (project.specialPermissionSet) {
        createSpecialPermissions(project);
      }

      return project;
    } catch (IOException eCreationException) {
      logger.error("Error in project creation", eCreationException);
      throw eCreationException;
    }
  }

  @Override
  public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
      OpenProjectData project) {
    Map<PROJECT_TEMPLATE, String> template = new HashMap<>();

    template.put(
        PROJECT_TEMPLATE.TEMPLATE_KEY,
        calculateJiraProjectTypeAndTemplateFromProjectType(
            project, JiraAdapter.JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey));

    template.put(
        PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY,
        calculateJiraProjectTypeAndTemplateFromProjectType(
            project, JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType));

    return template;
  }

  @Override
  public Map<String, String> createComponentsForProjectRepositories(
      OpenProjectData data, List<String> exclusions) {
    if (!createJiraComponents) {
      logger.debug("Not creating jira components for repo!, functionality disabled!");
      return new HashMap<>();
    }
    Preconditions.checkNotNull(data, "data input cannot be null");
    Preconditions.checkNotNull(data.projectKey, "project key cannot be null");
    String path = String.format("%s%s/component", jiraUri, jiraApiPath);

    Map<String, String> createdComponents = new HashMap<>();

    Map<String, Map<URL_TYPE, String>> repositories = data.repositories;
    if (repositories != null) {
      for (Entry<String, Map<URL_TYPE, String>> repo : repositories.entrySet()) {
        String href = repo.getValue().get(URL_TYPE.URL_BROWSE_HTTP);

        logger.debug("Found repo {} {} for project {} ", repo.getKey(), href, data.projectKey);

        if (exclusions != null && exclusions.contains(repo.getKey())) {
          logger.debug(
              "Not creating jira component for repo: {} because of exclusionList {}",
              repo.getKey(),
              exclusions);
          continue;
        } else {
          logger.debug("Creating jira component for repo: {}", repo.getKey());
        }

        Component component = new Component();
        component.setName(
            String.format(
                "Technology%s", repo.getKey().replace(data.projectKey.toLowerCase(), "")));
        component.setProject(data.projectKey);
        component.setDescription(
            String.format("Technology component %s stored at %s", repo.getKey(), href));
        try {
          // restClient.callHttp(path, component, false, RestClient.HTTP_VERB.POST, null);
          RestClientCall call = httpPost().url(path).body(component).returnType(null);
          restClient.execute(call);
          createdComponents.put(component.getName(), component.getDescription());
        } catch (HttpException httpEx) {
          String error =
              String.format(
                  "Could not create jira component for %s - error %s",
                  component.getName(), httpEx.getMessage());
          logger.error(error);

          if (httpEx.getResponseCode() == 401) {
            // if you get a 401 here - we can't reach the project, so stop
            break;
          }
        } catch (IOException componentEx) {
          String error =
              String.format(
                  "Could not create jira component for %s - error %s",
                  component.getName(), componentEx.getMessage());
          logger.error(error);
        }
      }
    }
    return createdComponents;
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();
    if (stage.equals(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION)
        || (!project.bugtrackerSpace && project.bugtrackerUrl == null)) {
      logger.debug("Project {} not affected from cleanup", project.projectKey);
      return leftovers;
    }
    String jiraProjectPath =
        String.format("%s%s/project/%s", jiraUri, jiraApiPath, project.projectKey);

    logger.debug(
        "Cleaning up bugtracker space: {} with url {}", project.projectKey, project.bugtrackerUrl);

    try {
      // restClient.callHttp(jiraProjectPath, null, true, HTTP_VERB.DELETE, null);
      RestClientCall callJiraProjectDelete = httpDelete().url(jiraProjectPath).returnType(null);
      restClient.execute(callJiraProjectDelete);

      project.bugtrackerUrl = null;
    } catch (Exception cex) {
      logger.error("Could not clean up jira project {} error: {}", project.projectKey, cex.getMessage());
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.BUGTRACKER_PROJECT, 1);
    }
      
    try {
      if (project.specialPermissionSet) {
        String permissionSchemeUrl = String.format("%s/permissionscheme", jiraProjectPath);

        RestClientCall callGetScheme =
            httpGet().url(permissionSchemeUrl).returnType(PermissionSchemeResponse.class);
        PermissionSchemeResponse permissionScheme = restClient.execute(callGetScheme);
        if (permissionScheme.getName().contains(project.projectKey)) {
          logger.debug(
              "Cleaning up permissionset {} {} - for project: {}",
              permissionScheme.getId(),
              permissionScheme.getName(), project.projectKey);
        } else {
          logger.debug(
              "NOT Cleaning up permissionset {} {}, because it's a standard one",
              permissionScheme.getId(),
              permissionScheme.getName());
          return leftovers;
        }

        String jiraPermissionSchemePath =
            String.format(
                "%s%s/permissionscheme/%s", jiraUri, jiraApiPath, permissionScheme.getId());

        // restClient.callHttp(jiraPermissionSchemePath, null, true, HTTP_VERB.DELETE, null);
        RestClientCall callPermissionSchemeDelete = 
            httpDelete().url(jiraPermissionSchemePath).returnType(null);
        restClient.execute(callPermissionSchemeDelete);
      }

    } catch (Exception cex) {
      logger.error("Could not clean up jira project permission set {} error: {}", project.projectKey, cex.getMessage());
      cex.printStackTrace();
      // the reason to NOT add it here - is that we have check code in #createSpecialPermissions
    }
    logger.debug(
        "Cleanup done - status: {} components are left ..", leftovers.size() == 0 ? 0 : leftovers);

    return leftovers;
  }

  private Collection<String> getProjectKeys() {
    logger.debug("Getting all visible jira project keys");
    String url = String.format("%s%s/project", jiraUri, jiraApiPath);
    try {

      RestClientCall call =
          httpGet().url(url).returnTypeReference(new TypeReference<List<JsonNode>>() {});
      List<JsonNode> execute = restClient.execute(call);
      return execute.stream().map(n -> n.path("key").textValue()).collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Error in getProjectKeys: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public boolean isSpecialPermissionSchemeEnabled() {
    return specialPermissionSchemeEnabled;
  }
}
