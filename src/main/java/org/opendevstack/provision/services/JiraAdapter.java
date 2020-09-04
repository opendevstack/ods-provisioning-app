package org.opendevstack.provision.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

  public static final String ADAPTER_NAME = "jira";

  public static final String JIRA_TEMPLATE_KEY_PREFIX = "jira.project.template.key.";
  public static final String JIRA_TEMPLATE_TYPE_PREFIX = "jira.project.template.type.";
  public static final String JIRA_TEMPLATE_ADD_PROJECT_PROPERTY_PREFIX =
      "jira.project.template.add-project-property.";
  public static final String JIRA_TEMPLATE_PROJECT_PROPERTY_ENDPOINT_TEMPLATE_PREFIX =
      "jira.project.template.project-property-endpoint-template.";
  public static final String JIRA_TEMPLATE_PROJECT_PROPERTY_PAYLOAD_TEMPLATE_PREFIX =
      "jira.project.template.project-property-payload-template.";

  // Pattern to use for project with id
  public static final String JIRA_API_PROJECTS = "project";
  public static final String JIRA_API_GROUPS_PICKER = "groups/picker";
  public static final String JIRA_API_USERS = "user";
  public static final String JIRA_API_MYPERMISSIONS = "mypermissions";

  public static final String BASE_PATTERN = "%s%s/";
  public static final String JIRA_API_PROJECTS_PATTERN = BASE_PATTERN + JIRA_API_PROJECTS;
  public static final String JIRA_API_PROJECTS_FILTER_PATTERN = JIRA_API_PROJECTS_PATTERN + "/%s";
  public static final String JIRA_API_GROUPS_PICKER_PATTERN = BASE_PATTERN + JIRA_API_GROUPS_PICKER;
  public static final String JIRA_API_USER_PATTERN = BASE_PATTERN + JIRA_API_USERS;
  public static final String JIRA_API_MYPERMISSIONS_PATTERN = BASE_PATTERN + JIRA_API_MYPERMISSIONS;

  public static final String PERMISSIONS_ADMINISTER_JSON_PATH =
      "/permissions/ADMINISTER/havePermission";

  private static final String USERNAME_PARAM = "username";
  private static final String QUERY_PARAM = "query";

  private static final String USER_KEY = "key";
  private static final String USER_NAME = "name";
  private static final String USER_EMAIL_ADDRESS = "emailAddress";

  @Value("${jira.api.path}")
  private String jiraApiPath;

  @Value("${jira.uri}")
  private String jiraUri;

  @Value("${jira.permission.filepattern}")
  private String jiraPermissionFilePattern;

  @Value("${jira.specialpermissionschema.enabled:true}")
  private boolean specialPermissionSchemeEnabled;

  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;

  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @Value("${jira.project.notification.scheme.id:10000}")
  private String jiraNotificationSchemeId;

  @Value("${jira.create.components:true}")
  private boolean createJiraComponents;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
  private String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Autowired private IODSAuthnzAdapter manager;

  @Autowired private ConfigurableEnvironment environment;

  @Qualifier("projectTemplateKeyNames")
  @Autowired
  private List<String> projectTemplateKeyNames;

  public JiraAdapter() {
    super(ADAPTER_NAME);
  }

  private LeanJiraProject createProjectInJira(OpenProjectData project, FullJiraProject toBeCreated)
      throws IOException {
    LeanJiraProject created;
    try {
      created = callJiraCreateProjectApi(toBeCreated);
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
        created = callJiraCreateProjectApi(toBeCreated);
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
    LeanJiraProject created = getRestClient().execute(clientCall);

    return created;
  }

  private void setPropertyInJiraProjectIfEnabled(
      LeanJiraProject project, String projectType, String webhooksecret) throws IOException {
    String webhookProxyUrl = resolveWebhookProxyUrl(project.getKey(), webhooksecret);

    String addProjectProperty =
        calculateJiraProjectTypeAndTemplateFromProjectType(
            projectType, JIRA_TEMPLATE_ADD_PROJECT_PROPERTY_PREFIX, "false");

    if (!Boolean.valueOf(addProjectProperty)) {
      logger.debug(
          "Project type not configure to setup a jira project property [projectKey={}, projectType={}]",
          project.key,
          projectType);
      return;
    }

    String endpointTemplate =
        environment.getProperty(
            JIRA_TEMPLATE_PROJECT_PROPERTY_ENDPOINT_TEMPLATE_PREFIX + projectType);

    if (null == endpointTemplate) {
      String message =
          String.format(
              "Add project property is enabled but no jira endpoint is defined to set the jira project property [projectKey=%s, projectType=%s]",
              project.key, projectType);
      throw new RemoteException(message);
    }

    String payloadTemplate =
        environment.getProperty(
            JIRA_TEMPLATE_PROJECT_PROPERTY_PAYLOAD_TEMPLATE_PREFIX + projectType);

    if (null == payloadTemplate) {
      String message =
          String.format(
              "Add project property is enabled but no jira endpoint payload is defined to set the jira project property [projectKey=%s, projectType=%s]",
              project.key, projectType);
      throw new RemoteException(message);
    }

    String payload = payloadTemplate.replace("%PROJECT_KEY%", project.getKey());
    payload = payload.replace("%PROPERTY_KEY%", "WEBHOOK_PROXY.URL");
    payload = payload.replace("%PROPERTY_VALUE%", "https://" + webhookProxyUrl);

    String endpoint = String.format(endpointTemplate, jiraUri);

    RestClientCall clientCall =
        httpPost()
            .url(endpoint)
            .body(payload)
            .header(Map.of("Content-Type", "application/json", "Accept", "application/json"));
    getRestClient().execute(clientCall);
  }

  private String resolveWebhookProxyUrl(String projectKey, String webhookProxySecret) {
    return String.format(
        projectOpenshiftJenkinsWebhookProxyNamePattern
            + "/build"
            + "?trigger_secret="
            + webhookProxySecret,
        projectKey,
        projectOpenshiftBaseDomain);
  }

  /**
   * Create permission set for jira project
   *
   * @param project the project
   * @return the number of created permission sets
   */
  public int createSpecialPermissions(OpenProjectData project) {
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
        singleScheme = getRestClient().execute(call);

        // update jira project
        path =
            String.format(
                "%s%s/project/%s/permissionscheme", jiraUri, jiraApiPath, project.projectKey);
        PermissionScheme small = new PermissionScheme();
        small.setId(singleScheme.getId());
        getRestClient().execute(httpPut().body(small).url(path).returnType(null));
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

  public FullJiraProject buildJiraProjectPojoFromApiProject(OpenProjectData projectData) {
    String templateKey =
        calculateJiraProjectTypeAndTemplateFromProjectType(
            projectData.projectType, JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey);

    String templateType =
        calculateJiraProjectTypeAndTemplateFromProjectType(
            projectData.projectType, JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType);

    if (jiraTemplateKey.equals(templateKey)) {
      // TODO: fix this... it is a side effect that could be difficult to find and debug!
      projectData.projectType = defaultProjectKey;
    }

    logger.debug(
        "Creating project of type: {} for project: {}", templateKey, projectData.projectKey);

    return new FullJiraProject(
        null,
        projectData.projectKey,
        projectData.projectName,
        projectData.description,
        projectData.projectAdminUser,
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
            ? String.format(JIRA_API_PROJECTS_PATTERN, jiraUri, jiraApiPath)
            : String.format(JIRA_API_PROJECTS_FILTER_PATTERN, jiraUri, jiraApiPath, filter);

    try {
      RestClientCall call =
          httpGet().url(url).returnTypeReference(new TypeReference<List<LeanJiraProject>>() {});
      List<LeanJiraProject> projects = getRestClient().execute(call);
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
    shortcutConfluence.setUrl(data.collaborationSpaceUrl);
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
        RestClientCall call = httpPost().url(path).body(shortcut).returnType(Shortcut.class);
        getRestClient().execute(call);
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
      String projectType, String templatePrefix, String defaultValue) {
    Preconditions.checkNotNull(templatePrefix, "no template prefix passed");
    Preconditions.checkNotNull(defaultValue, "no defaultValue passed");
    /*
     * if the type can be found in the global definition of types (projectTemplateKeyNames) and is
     * also configured for jira (environment.containsProperty) - take it, if not fall back to
     * default
     */
    return (projectType != null
            && environment.containsProperty(templatePrefix + projectType)
            && projectTemplateKeyNames.contains(projectType))
        ? environment.getProperty(templatePrefix + projectType)
        : defaultValue;
  }

  private Map<String, String> convertJiraProjectToKeyMap(List<LeanJiraProject> projects) {
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

      project.projectAdminUser = resolveProjectAdminUser(project);

      FullJiraProject toBeCreated = buildJiraProjectPojoFromApiProject(project);

      LeanJiraProject created = createProjectInJira(project, toBeCreated);

      setPropertyInJiraProjectIfEnabled(
          created, project.getProjectType(), project.webhookProxySecret);

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

  public String resolveProjectAdminUser(OpenProjectData project) {
    if (!(project.specialPermissionSet && isSpecialPermissionSchemeEnabled())
        || project.projectAdminUser == null
        || project.projectAdminUser.trim().length() == 0) {
      return getUserName();
    } else {
      return project.projectAdminUser;
    }
  }

  @Override
  public Map<PROJECT_TEMPLATE, String> retrieveInternalProjectTypeAndTemplateFromProjectType(
      OpenProjectData project) {
    Map<PROJECT_TEMPLATE, String> template = new HashMap<>();

    template.put(
        PROJECT_TEMPLATE.TEMPLATE_KEY,
        calculateJiraProjectTypeAndTemplateFromProjectType(
            project.projectType, JiraAdapter.JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey));

    template.put(
        PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY,
        calculateJiraProjectTypeAndTemplateFromProjectType(
            project.projectType, JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType));

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
                "Technology%s", repo.getKey().replaceFirst(data.projectKey.toLowerCase(), "")));
        component.setProject(data.projectKey);
        component.setDescription(
            String.format("Technology component %s stored at %s", repo.getKey(), href));
        try {
          RestClientCall call = httpPost().url(path).body(component).returnType(null);
          getRestClient().execute(call);
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
        String.format(JIRA_API_PROJECTS_FILTER_PATTERN, jiraUri, jiraApiPath, project.projectKey);

    logger.debug(
        "Cleaning up bugtracker space: {} with url {}", project.projectKey, project.bugtrackerUrl);

    try {
      RestClientCall callJiraProjectDelete = httpDelete().url(jiraProjectPath).returnType(null);
      getRestClient().execute(callJiraProjectDelete);

      project.bugtrackerUrl = null;
    } catch (Exception cex) {
      logger.error(
          "Could not clean up jira project {} error: {}", project.projectKey, cex.getMessage());
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.BUGTRACKER_PROJECT, 1);
    }

    try {
      if (project.specialPermissionSet) {
        String permissionSchemeUrl = String.format("%s/permissionscheme", jiraProjectPath);

        RestClientCall callGetScheme =
            httpGet().url(permissionSchemeUrl).returnType(PermissionSchemeResponse.class);
        PermissionSchemeResponse permissionScheme = getRestClient().execute(callGetScheme);
        if (permissionScheme.getName().contains(project.projectKey)) {
          logger.debug(
              "Cleaning up permissionset {} {} - for project: {}",
              permissionScheme.getId(),
              permissionScheme.getName(),
              project.projectKey);
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

        RestClientCall callPermissionSchemeDelete =
            httpDelete().url(jiraPermissionSchemePath).returnType(null);
        getRestClient().execute(callPermissionSchemeDelete);
      }

    } catch (Exception cex) {
      logger.error(
          "Could not clean up jira project permission set {} error: {}",
          project.projectKey,
          cex.getMessage());
      // the reason to NOT add it to the list here - is that we have check code in
      // #createSpecialPermissions
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
      List<JsonNode> execute = getRestClient().execute(call);
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

  @Override
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {

    try {
      Assert.notNull(newProject, "Parameter 'newProject' is null!");
      Assert.notNull(
          newProject.projectKey, "Properties 'projectKey' of parameter 'newProject' is null!");

      logger.info("checking create project preconditions for project '{}'!", newProject.projectKey);

      List<CheckPreconditionFailure> preconditionFailures =
          createProjectAdminUserExistsCheck(resolveProjectAdminUser(newProject))
              .andThen(createUserCanCreateProjectCheck(getUserName()))
              .andThen(createRequiredGroupExistsCheck(newProject))
              .andThen(createProjectKeyExistsCheck(newProject.projectKey))
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
      List<String> groups = new ArrayList<>();

      if (Boolean.TRUE.equals(newProject.specialPermissionSet)) {
        groups.addAll(newProject.specialPermissionSetGroups());
      }

      groups.add(globalKeyuserRoleName);

      groups.forEach(
          group -> {
            if (!checkGroupExists(group)) {
              String message = String.format("Group '%s' does not exists in Jira!", group);
              preconditionFailures.add(
                  CheckPreconditionFailure.getUnexistantGroupInstance(message));
            }
          });

      return preconditionFailures;
    };
  }

  private boolean checkGroupExists(String group) {

    logger.info("checking if group '{}' exists!", group);

    Map<String, String> params = new HashMap<>();
    params.put(QUERY_PARAM, group);

    String url = String.format(JIRA_API_GROUPS_PICKER_PATTERN, jiraUri, jiraApiPath);
    try {

      String response = null;

      try {
        response =
            getRestClient()
                .execute(httpGet().url(url).queryParams(params).returnType(String.class));
        Assert.notNull(response, "Response is null for '" + group + "'");
      } catch (HttpException e) {
        if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
          logger.debug("Group '{}' was not found!", group, e);
          return false;
        } else {
          logger.warn("Unexpected method trying to get group '{}'!", group, e);
          throw e;
        }
      }

      JsonNode json = new ObjectMapper().readTree(response);

      JsonNode haveGroup = json.at("/groups").findPath("name");

      if (MissingNode.class.isInstance(haveGroup)) {
        logger.warn("Missing node for '{}'!", json);
        return false;
      }

      return group.equalsIgnoreCase(haveGroup.asText());

    } catch (IOException e) {
      throw new AdapterException(e);
    }
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createProjectKeyExistsCheck(String projectKey) {
    return preconditionFailures -> {
      logger.info("Checking if projectKey '{}' exists in Jira!", projectKey);
      try {
        String path =
            String.format(JIRA_API_PROJECTS_FILTER_PATTERN, jiraUri, jiraApiPath, projectKey);
        getRestClient().execute(httpGet().url(path));
        String message =
            String.format("ProjectKey '%s' already exists in '%s'!", projectKey, ADAPTER_NAME);
        preconditionFailures.add(CheckPreconditionFailure.getProjectExistsInstance(message));
      } catch (HttpException exception) {
        if (exception.getResponseCode() == 404) {
          logger.debug(String.format("Could not find JIRA project %s", projectKey));
        } else {
          String message =
              String.format(
                  "Could not query JIRA for project key '%s' : '%s'!", projectKey, ADAPTER_NAME);
          preconditionFailures.add(CheckPreconditionFailure.getExceptionInstance(message));
        }
      } catch (IOException ioEx) {
        String message =
            String.format(
                "Could not query JIRA for project key '%s' : '%s'!", projectKey, ADAPTER_NAME);
        preconditionFailures.add(CheckPreconditionFailure.getExceptionInstance(message));
      }

      return preconditionFailures;
    };
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createProjectAdminUserExistsCheck(String user) {
    return preconditionFailures -> {
      logger.info("checking if user '{}' exists!", user);

      if (!checkUserExists(user)) {
        String message = String.format("User '%s' does not exists in '%s'!", user, ADAPTER_NAME);
        preconditionFailures.add(CheckPreconditionFailure.getUnexistantUserInstance(message));
      }

      return preconditionFailures;
    };
  }

  public Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>>
      createUserCanCreateProjectCheck(String username) {
    return preconditionFailures -> {
      logger.info("checking if user '{}' has permissions to create project in Jira!", username);

      String url = String.format(JIRA_API_MYPERMISSIONS_PATTERN, jiraUri, jiraApiPath);
      try {
        String response = getRestClient().execute(httpGet().url(url).returnType(String.class));
        Assert.notNull(response, "Response is null for '" + username + "'");

        JsonNode json = new ObjectMapper().readTree(response);

        String failureMessage =
            String.format(
                "User '%s' do not have permission 'ADMINISTER' which is required to create a project in Jira!",
                username);

        JsonNode havePermission = json.at(PERMISSIONS_ADMINISTER_JSON_PATH);

        if (MissingNode.class.isInstance(havePermission)) {
          logger.warn("Missing node for '{}'!", PERMISSIONS_ADMINISTER_JSON_PATH);
          preconditionFailures.add(
              CheckPreconditionFailure.getCreateProjectPermissionMissingInstance(failureMessage));
          return preconditionFailures;

        } else if (!havePermission.asBoolean()) {
          logger.debug(failureMessage);
          preconditionFailures.add(
              CheckPreconditionFailure.getCreateProjectPermissionMissingInstance(failureMessage));
          return preconditionFailures;
        }

        return preconditionFailures;

      } catch (IOException e) {
        throw new AdapterException(e);
      }
    };
  }

  private boolean checkUserExists(String username) {

    Map<String, String> params = new HashMap<>();
    params.put(USERNAME_PARAM, username);

    String url = String.format(JIRA_API_USER_PATTERN, jiraUri, jiraApiPath);
    try {
      String response = null;

      try {
        response =
            getRestClient()
                .execute(httpGet().url(url).queryParams(params).returnType(String.class));
        Assert.notNull(response, "Response is null for '" + username + "'");
      } catch (HttpException e) {
        if (HttpStatus.NOT_FOUND.value() == e.getResponseCode()) {
          logger.debug("User '{}' was not found!", username, e);
          return false;
        } else {
          logger.warn("Unexpected method trying to get user '{}'!", username, e);
          throw e;
        }
      }

      JsonNode json = new ObjectMapper().readTree(response);

      BiPredicate<String, String> findAndCompare = createFindPathAndCompare(json);

      return findAndCompare.test(USER_KEY, username)
          || findAndCompare.test(USER_NAME, username)
          || findAndCompare.test(USER_EMAIL_ADDRESS, username);

    } catch (IOException e) {
      throw new AdapterException(e);
    }
  }

  public static BiPredicate<String, String> createFindPathAndCompare(JsonNode json) {
    return (path, value) -> {
      JsonNode key = json.findPath(path);
      return key != null && key.asText().equalsIgnoreCase(value);
    };
  }

  public String getJiraNotificationSchemeId() {
    return jiraNotificationSchemeId;
  }

  public ConfigurableEnvironment getEnvironment() {
    return environment;
  }

  public void setCreateJiraComponents(boolean create) {
    createJiraComponents = create;
  }

  public String getGlobalKeyuserRoleName() {
    return globalKeyuserRoleName;
  }
}
