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

package org.opendevstack.provision.controller;

import static java.lang.String.format;
import static org.opendevstack.provision.controller.CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import org.opendevstack.provision.adapter.*;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.IServiceAdapter.CLEANUP_LEFTOVER_COMPONENTS;
import org.opendevstack.provision.adapter.IServiceAdapter.LIFECYCLE_STAGE;
import org.opendevstack.provision.adapter.IServiceAdapter.PROJECT_TEMPLATE;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.MissingCredentialsInfoException;
import org.opendevstack.provision.authentication.PreAuthorizeAllRoles;
import org.opendevstack.provision.authentication.PreAuthorizeOnlyAdministrator;
import org.opendevstack.provision.controller.CheckPreconditionsResponse.JobStage;
import org.opendevstack.provision.model.ExecutionJob;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.OpenProjectDataValidator;
import org.opendevstack.provision.model.jenkins.Job;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.StorageAdapter;
import org.opendevstack.provision.services.openshift.OpenshiftService;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.exception.ProjectAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

/** Rest Controller to handle the process of project creation */
@RestController
@RequestMapping(value = ProjectAPI.API_V2_PROJECT)
public class ProjectApiController {

  private static final Logger logger = LoggerFactory.getLogger(ProjectApiController.class);

  private static final String STR_LOGFILE_KEY = "loggerFileName";

  public static final String ADD_PROJECT_PARAM_NAME_ONLY_CHECK_PRECONDITIONS =
      "onlyCheckPreconditions";

  public static final String RETRY_AFTER_INTERVAL_IN_SECONDS = "180";
  public static final String RETRY_AFTER_HEADER = "Retry-After";

  public static final String CONFIG_PROVISION_ADD_PROJECT_CHECK_PRECONDITIONS =
      "provision.add-project.check-preconditions";
  public static final String PROJECT_TEMPLATE_KEYS = "project-template-keys";

  private static final String EMPTY_PROJECT_DESCRIPTION = "";

  private @Autowired IBugtrackerAdapter jiraAdapter;

  @Autowired(required = false)
  private ICollaborationAdapter confluenceAdapter;

  @Autowired(required = false)
  private OpenshiftService openshiftService;

  @Autowired private ISCMAdapter bitbucketAdapter;
  @Autowired private IJobExecutionAdapter jenkinsPipelineAdapter;
  @Autowired private MailAdapter mailAdapter;
  @Autowired private IStorage directStorage;

  @Autowired private IProjectIdentityMgmtAdapter projectIdentityMgmtAdapter;

  @Qualifier("projectTemplateKeyNames")
  @Autowired
  private List<String> projectTemplateKeyNames;

  @Autowired private StorageAdapter filteredStorage;

  @Autowired private IODSAuthnzAdapter manager;

  @Value("${openshift.project.upgrade}")
  private boolean ocUpgradeAllowed;

  // Explicity set to false to document default value
  @Value("${provision.cleanup.incomplete.projects:false}")
  private boolean cleanupAllowed;

  @Value("${" + CONFIG_PROVISION_ADD_PROJECT_CHECK_PRECONDITIONS + ":true}")
  private boolean checkPreconditionsEnabled;

  private String projectTemplateKeysAsJson;

  @Value("${adapters.confluence.enabled:true}")
  private boolean confluenceAdapterEnable;

  @Value("${services.openshift.enabled:true}")
  private boolean openshiftServiceEnable;

  @PostConstruct
  public void postConstruct() {
    logger.info(
        "Enable check preconditions = {} [{}={}]",
        checkPreconditionsEnabled,
        CONFIG_PROVISION_ADD_PROJECT_CHECK_PRECONDITIONS,
        checkPreconditionsEnabled);
  }

  /**
   * Create a new projectand process subsequent calls to dependent services, to create a complete
   * project stack.
   *
   * @param newProject the {@link OpenProjectData} containing the request information
   * @return the created project with additional information, e.g. links or in case an error
   *     happens, the error
   */
  @PreAuthorizeOnlyAdministrator
  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<Object> addProject(
      @RequestBody OpenProjectData newProject,
      @RequestParam(ADD_PROJECT_PARAM_NAME_ONLY_CHECK_PRECONDITIONS)
          Optional<Boolean> onlyCheckPreconditions,
      @RequestHeader(value = "Content-Type", required = false) String contentType) {

    if (newProject == null
        || newProject.getProjectKey() == null
        || newProject.getProjectKey().trim().length() == 0
        || newProject.getProjectName() == null
        || newProject.getProjectName().trim().length() == 0) {
      return ResponseEntity.badRequest()
          .body("Project key and name are mandatory fields to create a project!");
    }

    if (newProject.isSpecialPermissionSet() && !jiraAdapter.isSpecialPermissionSchemeEnabled()) {
      return ResponseEntity.badRequest()
          .body(
              format(
                  "Project with key %s can not be created with special permission set, "
                      + "since property jira.specialpermissionschema.enabled=false",
                  newProject.getProjectKey()));
    }

    newProject.setDescription(ProjectApiController.createShortenedDescription(newProject));

    newProject.setProjectKey(newProject.getProjectKey().toUpperCase());
    MDC.put(STR_LOGFILE_KEY, newProject.getProjectKey());

    try {
      logger.debug(
          "Project to be created: {}",
          new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(newProject));

      // verify the project does NOT exist
      if (directStorage.getProject(newProject.getProjectKey()) != null
          || directStorage.getProjectByName(newProject.getProjectName()) != null) {
        {
          throw new ProjectAlreadyExistsException(
              format(
                  "Project with key (%s) or name (%s) already exists",
                  newProject.getProjectKey(), newProject.getProjectName()));
        }
      }

      List<CheckPreconditionFailure> preconditionsFailures = new ArrayList<>();

      // TODO: move this block to check preconditions in projectIdentityMgmtAdapter
      if (newProject.isSpecialPermissionSet()) {
        if (!jiraAdapter.isSpecialPermissionSchemeEnabled()) {
          logger.info(
              "Do not validate special bugtracker permission set, "
                  + "because special permission sets are disabled in configuration");
        } else {
          try {
            projectIdentityMgmtAdapter.validateIdSettingsOfProject(newProject);
          } catch (IdMgmtException e) {
            preconditionsFailures.add(
                CheckPreconditionFailure.getUnexistantGroupInstance(e.getMessage()));
          }
        }
      }

      // If preconditions check fails an exception is thrown and cough below in try/catch block
      preconditionsFailures.addAll(checkPreconditions(newProject));

      // if list is not empty the precondition check failed
      if (!preconditionsFailures.isEmpty()) {

        String body =
            CheckPreconditionsResponse.checkPreconditionFailed(
                contentType, CHECK_PRECONDITIONS, preconditionsFailures);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(RETRY_AFTER_HEADER, RETRY_AFTER_INTERVAL_IN_SECONDS)
            .body(body);
      }

      if (onlyCheckPreconditions.orElse(Boolean.FALSE)) {

        return ResponseEntity.ok(
            CheckPreconditionsResponse.successful(contentType, CHECK_PRECONDITIONS));
      }

      // init webhook secret
      newProject.setWebhookProxySecret(UUID.randomUUID().toString());

      if (newProject.isBugtrackerSpace()) {
        // create the bugtracker project
        newProject = jiraAdapter.createBugtrackerProjectForODSProject(newProject);

        Preconditions.checkNotNull(
            newProject.getBugtrackerUrl(),
            jiraAdapter.getClass() + " did not return bugTracker url");

        // create confluence space
        if (isConfluenceAdapterEnable()) {
          newProject.setCollaborationSpaceUrl(
              confluenceAdapter.createCollaborationSpaceForODSProject(newProject));

          Preconditions.checkNotNull(
              newProject.getCollaborationSpaceUrl(),
              confluenceAdapter.getClass() + " did not return collabSpace url");
        }

        logger.debug(
            "Updated project with collaboration information:\n {}",
            new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(newProject));
      }

      // create the delivery chain, including scm repos, and platform project
      newProject = createDeliveryChain(newProject);

      // add shortcuts into the space
      jiraAdapter.addShortcutsToProject(newProject);

      // store the project data
      String filePath = directStorage.storeProject(newProject);
      if (filePath != null) {
        logger.debug("Project {} successfully stored: {}", newProject.getProjectKey(), filePath);
      }

      // notify user via mail of project creation with embedding links
      mailAdapter.notifyUsersAboutProject(newProject);

      return ResponseEntity.ok().body(newProject);
    } catch (ProjectAlreadyExistsException exAlreadyExists) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(exAlreadyExists.getMessage());
    } catch (CreateProjectPreconditionException cppe) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .header(RETRY_AFTER_HEADER, RETRY_AFTER_INTERVAL_IN_SECONDS)
          .body(formatError(contentType, CHECK_PRECONDITIONS, cppe.getMessage()));
    } catch (Exception exProvisionNew) {
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupResults =
          cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, newProject);

      String error =
          (cleanupResults.size() == 0)
              ? format(
                  "An error occured while creating project [%s], reason [%s]"
                      + " - but all cleaned up!",
                  newProject.getProjectKey(), exProvisionNew.getMessage())
              : format(
                  "An error occured while creating project [%s], reason [%s]"
                      + " - cleanup attempted, but [%s] components are still there!",
                  newProject.getProjectKey(), exProvisionNew.getMessage(), cleanupResults);

      logger.error(error, exProvisionNew);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } finally {
      MDC.remove(STR_LOGFILE_KEY);
    }
  }

  /**
   * @param contentType
   * @param jobStage
   * @param error
   * @return a json string if the content type is json, otherwise it returns the error parameter
   */
  public static String formatError(String contentType, JobStage jobStage, String error) {
    if (CheckPreconditionsResponse.isJsonContentType(contentType)) {
      return CheckPreconditionsResponse.failedAsJson(jobStage, error);
    }
    return error.toString();
  }

  /**
   * @param newProject
   * @return list of precondition check failure messages
   * @throws CreateProjectPreconditionException if an exception was captured while checking the
   *     preconditions
   */
  private List<CheckPreconditionFailure> checkPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {

    if (!isCheckPreconditionsEnabled()) {
      logger.info(
          "Skipping check preconditions! Check preconditions is disabled [{}={}]",
          CONFIG_PROVISION_ADD_PROJECT_CHECK_PRECONDITIONS,
          checkPreconditionsEnabled);
      return new ArrayList<>();
    }

    final List<CheckPreconditionFailure> results = new ArrayList<>();

    if (newProject.isBugtrackerSpace()) {

      List<IServiceAdapter> adapters = new ArrayList<>();
      adapters.add(jiraAdapter);

      if (isConfluenceAdapterEnable()) {
        adapters.add(confluenceAdapter);
      }

      for (IServiceAdapter adapter : adapters) {
        results.addAll(adapter.checkCreateProjectPreconditions(newProject));
      }
    }

    if (newProject.isPlatformRuntime()) {
      results.addAll(bitbucketAdapter.checkCreateProjectPreconditions(newProject));

      if (isOpenshiftServiceEnable() && null != openshiftService) {
        results.addAll(openshiftService.checkCreateProjectPreconditions(newProject));
      }
    }

    if (results.isEmpty()) {
      logger.info(
          "Done with all create project preconditions checks for project [project={}]!",
          newProject.getProjectKey());
    } else {
      logger.info(
          "Done with all create project preconditions checks for project but some checks failed! [project={}, failures={}]!",
          newProject.getProjectKey(),
          results);
    }

    return Collections.unmodifiableList(results);
  }

  private boolean checkPreconditionsEnabled() {
    return checkPreconditionsEnabled;
  }

  /**
   * Update a project, e.g. add new quickstarters, upgrade a bugtracker only project
   *
   * @param updatedProject the project containing the update data
   * @return the updated project
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.PUT)
  public ResponseEntity<Object> updateProject(@RequestBody OpenProjectData updatedProject) {

    if (updatedProject == null || updatedProject.getProjectKey().trim().length() == 0) {
      return ResponseEntity.badRequest().body("Project key is mandatory to call update project!");
    }
    MDC.put(STR_LOGFILE_KEY, updatedProject.getProjectKey());

    logger.debug("Update project {}", updatedProject.getProjectKey());
    try {
      logger.debug(
          "Project: {}",
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(updatedProject));

      OpenProjectData storedExistingProject =
          directStorage.getProject(updatedProject.getProjectKey());

      if (storedExistingProject == null) {
        return ResponseEntity.notFound().build();
      }

      // in case only quickstarters are passed - we are setting the upgrade flag
      if (updatedProject.getQuickstarters() != null
          && updatedProject.getQuickstarters().size() > 0) {
        updatedProject.setPlatformRuntime(true);
      }

      validateQuickstarters(
          updatedProject, OpenProjectDataValidator.API_COMPONENT_ID_VALIDATOR_LIST);

      // add the baseline, to return a full project later
      updatedProject.setDescription(storedExistingProject.getDescription());
      updatedProject.setProjectName(storedExistingProject.getProjectName());
      updatedProject.setWebhookProxySecret(storedExistingProject.getWebhookProxySecret());

      // add the scm url & bugtracker space bool
      updatedProject.setScmvcsUrl(storedExistingProject.getScmvcsUrl());
      updatedProject.setBugtrackerSpace(storedExistingProject.isBugtrackerSpace());
      // we purposely allow overwriting platformRuntime settings, to create a project later
      // on
      if (!storedExistingProject.isPlatformRuntime()
          && updatedProject.isPlatformRuntime()
          && !ocUpgradeAllowed) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                "Project: "
                    + updatedProject.getProjectKey()
                    + " cannot be upgraded to openshift usage");
      } else if (storedExistingProject.isPlatformRuntime()) {
        // we need to set this, otherwise the provisioning later will not work
        updatedProject.setPlatformRuntime(storedExistingProject.isPlatformRuntime());
      }

      // add (hard) permission data
      if (storedExistingProject.isSpecialPermissionSet()) {
        updatedProject.setSpecialPermissionSet(storedExistingProject.isSpecialPermissionSet());
        updatedProject.setProjectAdminUser(storedExistingProject.getProjectAdminUser());
        updatedProject.setProjectAdminGroup(storedExistingProject.getProjectAdminGroup());
        updatedProject.setProjectUserGroup(storedExistingProject.getProjectUserGroup());
        updatedProject.setProjectReadonlyGroup(storedExistingProject.getProjectReadonlyGroup());
      }

      updatedProject = createDeliveryChain(updatedProject);

      /*
       * add the already existing data /provisioned/ + we have to add the scm url here, in case we
       * have upgraded a bugtracker only project to a platform project
       */
      storedExistingProject.setScmvcsUrl(updatedProject.getScmvcsUrl());
      if (updatedProject.getQuickstarters() != null) {
        if (storedExistingProject.getQuickstarters() != null) {
          storedExistingProject.getQuickstarters().addAll(updatedProject.getQuickstarters());
        } else {
          storedExistingProject.setQuickstarters(updatedProject.getQuickstarters());
        }
      }

      if (storedExistingProject.getRepositories() != null
          && updatedProject.getRepositories() != null) {
        storedExistingProject.getRepositories().putAll(updatedProject.getRepositories());
      } else if (updatedProject.getRepositories() != null) {
        storedExistingProject.setRepositories(storedExistingProject.getRepositories());
      }

      // add the new executions - so people can track what's going on
      storedExistingProject.setLastExecutionJobs(updatedProject.getLastExecutionJobs());

      // store the updated project
      if (directStorage.updateStoredProject(storedExistingProject)) {
        logger.debug("project {} successfully updated", updatedProject.getProjectKey());
      }

      // notify user via mail of project updates with embedding links
      mailAdapter.notifyUsersAboutProject(storedExistingProject);

      return ResponseEntity.ok().body(storedExistingProject);
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(iae.getMessage());
    } catch (MissingCredentialsInfoException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception exProvision) {
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupResults =
          cleanup(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION, updatedProject);

      String error =
          (cleanupResults.size() == 0)
              ? format(
                  "An error occured while updating project %s, reason %s"
                      + " - but all cleaned up!",
                  updatedProject.getProjectKey(), exProvision.getMessage())
              : format(
                  "An error occured while updating project %s, reason %s"
                      + " - cleanup attempted, but [%s] components are still there!",
                  updatedProject.getProjectKey(), exProvision.getMessage(), cleanupResults);

      logger.error(error);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } finally {
      MDC.remove(STR_LOGFILE_KEY);
    }
  }

  /**
   * @param updatedProject
   * @param validators
   * @throws IllegalArgumentException if any validator rule do not accept any quickstarters
   */
  public static void validateQuickstarters(
      @RequestBody OpenProjectData updatedProject, List<Consumer<Map<String, String>>> validators) {
    Assert.notNull(updatedProject, "Parameter updatedProject must not be null!");
    Assert.notEmpty(validators, "Parameter validators must not be null!");
    validators.forEach(
        validator -> {
          updatedProject.getQuickstarters().stream().forEach(validator);
        });
  }

  /**
   * Create the delivery chain within the platform in case {@link
   * OpenProjectData#getPlatformRuntime} is set to true
   *
   * @param project the meta information from the API
   * @return the generated, amended Project
   * @throws IOException
   * @throws Exception in case something goes wrong
   */
  private OpenProjectData createDeliveryChain(OpenProjectData project) throws IOException {
    logger.debug(
        "Create delivery chain for: {}, platform? {}, create scm: {}",
        project.getProjectKey(),
        project.isPlatformRuntime(),
        (project.getScmvcsUrl() == null ? true : false));

    if (!project.isPlatformRuntime()) {
      return project;
    }

    // create auxilaries - for design and for the ocp artifacts
    String[] auxiliaryRepositories = {"occonfig-artifacts", "design"};

    if (project.getScmvcsUrl() == null) {
      // create the bugtracker project
      project.setScmvcsUrl(bitbucketAdapter.createSCMProjectForODSProject(project));

      Preconditions.checkNotNull(
          project.getScmvcsUrl(), bitbucketAdapter.getClass() + " did not return scmvcs url");

      project.setRepositories(
          bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(
              project, auxiliaryRepositories));

      // provision platform projects
      project = jenkinsPipelineAdapter.createPlatformProjects(project);
    }

    int newQuickstarters =
        (project.getQuickstarters() == null ? 0 : project.getQuickstarters().size());

    int existingComponentRepos =
        (project.getRepositories() == null ? 0 : project.getRepositories().size());

    // create repositories dependent of the chosen quickstarters
    Map<String, Map<URL_TYPE, String>> newComponentRepos =
        bitbucketAdapter.createComponentRepositoriesForODSProject(project);
    if (newComponentRepos != null) {
      if (project.getRepositories() != null) {
        project.getRepositories().putAll(newComponentRepos);
      } else {
        project.setRepositories(newComponentRepos);
      }
    }

    logger.debug(
        "New quickstarters {}, prior existing repos: {}, now project repos: {}",
        newQuickstarters,
        existingComponentRepos,
        project.getRepositories().size());

    Preconditions.checkState(
        project.getRepositories().size() == existingComponentRepos + newQuickstarters,
        format(
            "Class: %s did not create %s new repositories "
                + "for new quickstarters, existing repos: %s",
            bitbucketAdapter.getClass(), newQuickstarters, existingComponentRepos));

    // based on the (changed) repository names, update the
    // quickstarters
    addRepositoryUrlsToQuickstarters(project);

    List<String> auxiliariesToExclude = new ArrayList<>();
    final String projectKey = project.getProjectKey();
    Arrays.asList(auxiliaryRepositories)
        .forEach(
            repoName ->
                auxiliariesToExclude.add(
                    bitbucketAdapter.createRepoNameFromComponentName(projectKey, repoName)));

    // create jira components from newly created repos
    if (project.isBugtrackerSpace()) {
      jiraAdapter.createComponentsForProjectRepositories(project, auxiliariesToExclude);
    } else {
      logger.info(
          "Do not create bugtracker components for {}, since it has no bugtragger space configured!",
          project.getProjectKey());
    }

    // add the long running execution links from the
    // IJobExecutionAdapter, so the consumer can track them
    if (project.getLastExecutionJobs() == null) {
      project.setLastExecutionJobs(new ArrayList<>());
    }
    List<ExecutionsData> jobs =
        jenkinsPipelineAdapter.provisionComponentsBasedOnQuickstarters(project);
    logger.debug("New quickstarter job executions: {}", jobs.size());

    for (ExecutionsData singleJob : jobs) {
      project
          .getLastExecutionJobs()
          .add(new ExecutionJob(singleJob.getJobName(), singleJob.getPermalink()));
    }

    return project;
  }

  /**
   * Get a list with all projects in the ODS prov system. In this case the quickstarters {@link
   * OpenProjectData#getQuickstarters} contain also the description of the quickstarter that was
   * used
   */
  @PreAuthorizeAllRoles
  @GetMapping
  public ResponseEntity<Map<String, OpenProjectInfo>> getAllProjects() {

    try {
      Map<String, OpenProjectData> projectsData = filteredStorage.getProjects();
      Map<String, OpenProjectInfo> projects = new HashMap<>();

      for (Map.Entry<String, OpenProjectData> entry : projectsData.entrySet()) {
        projects.put(entry.getKey(), new OpenProjectInfo(entry.getValue()));
      }

      return ResponseEntity.ok(projects);

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Get a list with all projects in the ODS prov system defined by their key. In this case the
   * quickstarters {@link OpenProjectData#getQuickstarters} contain also the description of the
   * quickstarter that was used
   *
   * @param id the project's key
   * @return Response with a complete project list of {@link OpenProjectData}
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  public ResponseEntity<OpenProjectData> getProject(@PathVariable String id) {
    OpenProjectData project = filteredStorage.getFilteredSingleProject(id);

    if (project == null) {
      return ResponseEntity.notFound().build();
    }

    if (project.getQuickstarters() != null) {
      List<Map<String, String>> enhancedStarters = new ArrayList<>();

      for (Map<String, String> quickstarters : project.getQuickstarters()) {
        String componentType = quickstarters.get(OpenProjectData.COMPONENT_TYPE_KEY);

        String componentDescription =
            jenkinsPipelineAdapter
                .getComponentByType(componentType)
                .map(Job::getDescription)
                .orElse(componentType);
        quickstarters.put(OpenProjectData.COMPONENT_DESC_KEY, componentDescription);
        enhancedStarters.add(quickstarters);
      }
      project.setQuickstarters(enhancedStarters);
    }
    return ResponseEntity.ok(project);
  }

  /**
   * Validate the project name. Duplicates are not allowed in most bugtrackers.
   *
   * @param name the project's name
   * @return Response with HTTP status. If 406 a project with this name exists in JIRA
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/validate")
  public ResponseEntity<Object> validateProject(@RequestParam(value = "projectName") String name) {
    if (jiraAdapter.projectKeyExists(name)) {
      HashMap<String, Object> result = new HashMap<>();
      result.put("error", true);
      result.put("error_message", "A project with this name exists");
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(result);
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Get all available (project) template keys, which can be used later in {@link
   * OpenProjectData#getProjectType}
   *
   * @return a list of available template keys
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/templates")
  public ResponseEntity<String> getProjectTemplateKeys(
      @RequestHeader(value = "Accept", required = false) String acceptType) {
    try {
      if (null == acceptType
          || MediaType.TEXT_PLAIN.isCompatibleWith(MediaType.parseMediaType(acceptType))) {
        return ResponseEntity.ok(
            ProjectTemplateType.createProjectTemplateKeysAsString(projectTemplateKeyNames));
      } else if (MediaType.APPLICATION_JSON.isCompatibleWith(
          MediaType.parseMediaType(acceptType))) {
        String json = getProjectTemplateKeysAsJson();
        return ResponseEntity.ok(json);
      } else {
        ObjectMapper mapper = new ObjectMapper();
        String body =
            mapper.writeValueAsString(Map.of("ERROR:", "Unsupported accept type: " + acceptType));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
      }
    } catch (JsonProcessingException e) {
      logger.error(e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private String getProjectTemplateKeysAsJson() throws JsonProcessingException {
    if (projectTemplateKeysAsJson == null) {
      projectTemplateKeysAsJson = createProjectTemplateKeysJson(projectTemplateKeyNames);
    }
    return projectTemplateKeysAsJson;
  }

  public static String createProjectTemplateKeysJson(List<String> projectTemplateKeys)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    List<ProjectTemplateType> list = ProjectTemplateType.createList(projectTemplateKeys);
    Map<String, List<ProjectTemplateType>> map = new HashMap();
    map.put(PROJECT_TEMPLATE_KEYS, list);
    return mapper.writeValueAsString(map);
  }

  /**
   * Retrieve the underlying templates from {@link IBugtrackerAdapter} and {@link
   * ICollaborationAdapter}
   *
   * @param key the project type as in {@link OpenProjectData#getProjectKey}
   * @return a map with the templates (which are implementation specific)
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/template/{key}")
  public ResponseEntity<Map<String, String>> getProjectTypeTemplatesForKey(
      @PathVariable String key) {
    Map<String, String> templatesForKey = new HashMap<>();
    logger.debug("retrieving templates for key: " + key);

    Preconditions.checkNotNull(key, "Null template key is not allowed");

    OpenProjectData project = new OpenProjectData();
    project.setProjectType(key);

    Map<PROJECT_TEMPLATE, String> templates =
        jiraAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    templatesForKey.put(
        "bugTrackerTemplate",
        templates.get(PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY)
            + "#"
            + templates.get(PROJECT_TEMPLATE.TEMPLATE_KEY));

    if (isConfluenceAdapterEnable()) {
      templatesForKey.put(
          "collabSpaceTemplate",
          confluenceAdapter
              .retrieveInternalProjectTypeAndTemplateFromProjectType(project)
              .get(PROJECT_TEMPLATE.TEMPLATE_KEY));
    }

    return ResponseEntity.ok(templatesForKey);
  }

  /**
   * Validate the project's key name. Duplicates are not allowed in most bugtrackers.
   *
   * @param key the project's name to validate against
   * @return Response with HTTP status. If 406 a project with this key exists in JIRA
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/key/validate")
  public ResponseEntity<Object> validateKey(@RequestParam(value = "projectKey") String key) {
    if (jiraAdapter.projectKeyExists(key)) {
      HashMap<String, Object> result = new HashMap<>();
      result.put("error", true);
      result.put("error_message", "A key with this name exists");
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(result);
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Generate the Key on server side
   *
   * @param name the project name to generate the key from
   * @return the generated key
   */
  @PreAuthorizeAllRoles
  @RequestMapping(method = RequestMethod.GET, value = "/key/generate")
  public ResponseEntity<Map<String, String>> generateKey(
      @RequestParam(value = "name") String name) {
    Map<String, String> proj = new HashMap<>();
    proj.put("projectKey", jiraAdapter.buildProjectKey(name));
    return ResponseEntity.ok(proj);
  }

  /**
   * @param project
   * @return empty String if parameter is null!
   */
  public static String createShortenedDescription(OpenProjectData project) {
    Assert.notNull(project, "Parameter project is null!");
    if (project != null
        && project.getDescription() != null
        && project.getDescription().length() > 100) {
      return project.getDescription().substring(0, 99);
    }
    return project.getDescription();
  }

  private void addRepositoryUrlsToQuickstarters(OpenProjectData project) {
    if (project.getQuickstarters() == null || project.getRepositories() == null) {
      logger.debug("Repository Url mgmt - nothing to do on project {}", project.getProjectKey());
      return;
    }

    for (Map<String, String> option : project.getQuickstarters()) {
      logger.debug("options: " + option);

      String projectComponentKey = option.get(OpenProjectData.COMPONENT_ID_KEY);

      // recreate the repo name that the scmadapter (hopefully)
      // also used
      String repoName =
          bitbucketAdapter.createRepoNameFromComponentName(
              project.getProjectKey(), projectComponentKey);

      logger.debug(
          format("Trying to find repo %s in %s", repoName, project.getRepositories().keySet()));

      Map<URL_TYPE, String> repoUrls = project.getRepositories().get(repoName);

      if (repoUrls == null) {
        return;
      }

      option.put("git_url_ssh", repoUrls.get(URL_TYPE.URL_CLONE_SSH));
      option.put("git_url_http", repoUrls.get(URL_TYPE.URL_CLONE_HTTP));
    }
  }

  @PreAuthorizeOnlyAdministrator
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  public ResponseEntity<Object> deleteProject(@PathVariable String id) throws IOException {
    OpenProjectData project = filteredStorage.getFilteredSingleProject(id);

    if (!isCleanupAllowed()) {
      throw new IOException("Cleanup of projects is NOT allowed");
    }

    if (project == null) {
      return ResponseEntity.notFound().build();
    }

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers =
        cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, project);

    if (!leftovers.isEmpty()) {
      String error =
          format(
              "Could not delete all components of project %s - leftovers %s",
              project.getProjectKey(), leftovers);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } else {
      return ResponseEntity.ok().build();
    }
  }

  @PreAuthorizeOnlyAdministrator
  @RequestMapping(method = RequestMethod.DELETE)
  public ResponseEntity<Object> deleteComponents(@RequestBody OpenProjectData deletableComponents)
      throws IOException {
    if (!isCleanupAllowed()) {
      throw new IOException("Cleanup of projects is NOT allowed");
    }

    Preconditions.checkNotNull(deletableComponents, "Cannot delete null components");
    Preconditions.checkNotNull(
        deletableComponents.getQuickstarters(), "No quickstarters to delete are passed");

    OpenProjectData project =
        filteredStorage.getFilteredSingleProject(deletableComponents.getProjectKey());

    if (project == null) {
      return ResponseEntity.notFound().build();
    }

    // Quick fix to pass the webhook proxy secret from project
    deletableComponents.setWebhookProxySecret(project.getWebhookProxySecret());

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers =
        cleanup(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION, deletableComponents);

    if (!leftovers.isEmpty()) {
      String error =
          format(
              "Could not delete all components of project " + " %s - leftovers %s",
              project.getProjectKey(), leftovers);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } else {
      project.removeQuickstartersFromProject(deletableComponents.getQuickstarters());
      project.setLastExecutionJobs(deletableComponents.getLastExecutionJobs());
      this.directStorage.updateStoredProject(project);
      return ResponseEntity.ok(project);
    }
  }

  /**
   * In case something breaks during provisioniong, this method is called
   *
   * @param stage the lifecycle stage
   * @param project the project including any created information
   */
  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {

    if (!isCleanupAllowed()) {
      return new HashMap<>();
    }

    logger.error("Starting cleanup of project {} in phase {}", project.getProjectKey(), stage);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> notCleanedUpComponents = new HashMap<>();

    notCleanedUpComponents.putAll(jenkinsPipelineAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(bitbucketAdapter.cleanup(stage, project));

    if (isConfluenceAdapterEnable()) {
      notCleanedUpComponents.putAll(confluenceAdapter.cleanup(stage, project));
    }

    notCleanedUpComponents.putAll(jiraAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(filteredStorage.cleanup(stage, project));

    logger.debug(
        "Overall cleanup status of project: {} components left",
        notCleanedUpComponents.size() == 0 ? 0 : notCleanedUpComponents);

    return notCleanedUpComponents;
  }

  public void setCleanupAllowed(boolean cleanupAllowed) {
    this.cleanupAllowed = cleanupAllowed;
  }

  public boolean isCleanupAllowed() {
    return cleanupAllowed;
  }

  public void setDirectStorage(IStorage storage) {
    this.directStorage = storage;
  }

  public IStorage getDirectStorage() {
    return directStorage;
  }

  public boolean isOpenshiftServiceEnable() {
    return openshiftServiceEnable;
  }

  public void setOpenshiftServiceEnable(boolean openshiftServiceEnable) {
    this.openshiftServiceEnable = openshiftServiceEnable;
  }

  public boolean isConfluenceAdapterEnable() {
    return confluenceAdapterEnable;
  }

  public void setConfluenceAdapterEnable(boolean confluenceAdapterEnable) {
    this.confluenceAdapterEnable = confluenceAdapterEnable;
  }

  public void setConfluenceAdapter(ICollaborationAdapter confluenceAdapter) {
    this.confluenceAdapter = confluenceAdapter;
  }

  public ICollaborationAdapter getConfluenceAdapter() {
    return confluenceAdapter;
  }

  public boolean isCheckPreconditionsEnabled() {
    return checkPreconditionsEnabled;
  }

  public void setCheckPreconditionsEnabled(boolean checkPreconditionsEnabled) {
    this.checkPreconditionsEnabled = checkPreconditionsEnabled;
  }

  public boolean isOcUpgradeAllowed() {
    return ocUpgradeAllowed;
  }

  public void setOcUpgradeAllowed(boolean ocUpgradeAllowed) {
    this.ocUpgradeAllowed = ocUpgradeAllowed;
  }
}
