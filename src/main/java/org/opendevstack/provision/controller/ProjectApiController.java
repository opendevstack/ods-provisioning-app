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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter.CLEANUP_LEFTOVER_COMPONENTS;
import org.opendevstack.provision.adapter.IServiceAdapter.LIFECYCLE_STAGE;
import org.opendevstack.provision.adapter.IServiceAdapter.PROJECT_TEMPLATE;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.StorageAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller to handle the process of project creation
 *
 * @author Torsten Jaeschke
 * @author Clemens Utschig
 */
@RestController
@RequestMapping(value = "api/v2/project")
public class ProjectApiController {
  private static final Logger logger = LoggerFactory.getLogger(ProjectApiController.class);

  private static final String STR_LOGFILE_KEY = "loggerFileName";

  @Autowired IBugtrackerAdapter jiraAdapter;
  @Autowired ICollaborationAdapter confluenceAdapter;
  @Autowired private ISCMAdapter bitbucketAdapter;
  @Autowired private IJobExecutionAdapter rundeckAdapter;
  @Autowired private MailAdapter mailAdapter;
  @Autowired private IStorage directStorage;

  @Autowired private IProjectIdentityMgmtAdapter projectIdentityMgmtAdapter;

  @Autowired private List<String> projectTemplateKeyNames;

  @Autowired private StorageAdapter filteredStorage;

  // open for testing
  @Autowired IODSAuthnzAdapter manager;

  // open for testing
  @Value("${openshift.project.upgrade}")
  boolean ocUpgradeAllowed;

  @Value("${provision.cleanup.incomplete.projects:true}")
  boolean cleanupAllowed;

  /**
   * Create a new projectand process subsequent calls to dependent services, to create a complete
   * project stack.
   *
   * @param newProject the {@link OpenProjectData} containing the request information
   * @return the created project with additional information, e.g. links or in case an error
   *     happens, the error
   */
  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<Object> addProject(@RequestBody OpenProjectData newProject) {

    if (newProject == null
        || newProject.projectKey == null
        || newProject.projectKey.trim().length() == 0
        || newProject.projectName == null
        || newProject.projectName.trim().length() == 0) {
      return ResponseEntity.badRequest()
          .body("Project key and name are mandatory fields to create a project!");
    }

    if (newProject.specialPermissionSet && !jiraAdapter.isSpecialPermissionSchemeEnabled()) {
      return ResponseEntity.badRequest()
          .body(
              format(
                  "Project with key %s can not be created with special permission set, "
                      + "since property jira.specialpermissionschema.enabled=false",
                  newProject.projectKey));
    }

    // fix for opendevstack/ods-provisioning-app/issues/64
    shortenDescription(newProject);

    newProject.projectKey = newProject.projectKey.toUpperCase();
    MDC.put(STR_LOGFILE_KEY, newProject.projectKey);

    try {
      logger.debug(
          "Project to be created: {}",
          new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(newProject));

      if (newProject.specialPermissionSet) {
        if (!jiraAdapter.isSpecialPermissionSchemeEnabled()) {
          logger.info(
              "Do not validate special bugtracker permission set, "
                  + "because special permission sets are disabled in configuration");
        } else {
          projectIdentityMgmtAdapter.validateIdSettingsOfProject(newProject);
        }
      }

      // verify the project does NOT exist
      OpenProjectData projectLoad = directStorage.getProject(newProject.projectKey);
      if (projectLoad != null) {
        {
          throw new IOException(
              format(
                  "Project with key (%s) already exists: (%s)",
                  newProject.projectKey, projectLoad.projectKey));
        }
      }

      if (newProject.bugtrackerSpace) {
        // create the bugtracker project
        newProject = jiraAdapter.createBugtrackerProjectForODSProject(newProject);

        Preconditions.checkNotNull(
            newProject.bugtrackerUrl, jiraAdapter.getClass() + " did not return bugTracker url");

        // create confluence space
        newProject.collaborationSpaceUrl =
            confluenceAdapter.createCollaborationSpaceForODSProject(newProject);

        Preconditions.checkNotNull(
            newProject.collaborationSpaceUrl,
            confluenceAdapter.getClass() + " did not return collabSpace url");

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
        logger.debug("Project {} successfully stored: {}", newProject.projectKey, filePath);
      }

      // notify user via mail of project creation with embedding links
      mailAdapter.notifyUsersAboutProject(newProject);

      return ResponseEntity.ok().body(newProject);
    } catch (Exception exProvisionNew) {
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupResults =
          cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, newProject);

      String error =
          (cleanupResults.size() == 0)
              ? format(
                  "An error occured while creating project %s, reason %s"
                      + " - but all cleaned up!",
                  newProject.projectKey, exProvisionNew.getMessage())
              : format(
                  "An error occured while creating project %s, reason %s"
                      + " - cleanup attempted, but [%s] components are still there!",
                  newProject.projectKey, exProvisionNew.getMessage(), cleanupResults);

      logger.error(error, exProvisionNew);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } finally {
      MDC.remove(STR_LOGFILE_KEY);
    }
  }

  /**
   * Update a project, e.g. add new quickstarters, upgrade a bugtracker only project
   *
   * @param updatedProject the project containing the update data
   * @return the updated project
   */
  @RequestMapping(method = RequestMethod.PUT)
  public ResponseEntity<Object> updateProject(@RequestBody OpenProjectData updatedProject) {

    if (updatedProject == null || updatedProject.projectKey.trim().length() == 0) {
      return ResponseEntity.badRequest().body("Project key is mandatory to call update project!");
    }
    MDC.put(STR_LOGFILE_KEY, updatedProject.projectKey);

    logger.debug("Update project {}", updatedProject.projectKey);
    try {
      logger.debug(
          "Project: {}",
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(updatedProject));

      OpenProjectData storedExistingProject = directStorage.getProject(updatedProject.projectKey);

      if (storedExistingProject == null) {
        return ResponseEntity.notFound().build();
      }

      // add the baseline, to return a full project later
      updatedProject.description = storedExistingProject.description;
      updatedProject.projectName = storedExistingProject.projectName;
      updatedProject.webhookProxySecret = storedExistingProject.webhookProxySecret;

      // add the scm url & bugtracker space bool
      updatedProject.scmvcsUrl = storedExistingProject.scmvcsUrl;
      updatedProject.bugtrackerSpace = storedExistingProject.bugtrackerSpace;
      // we purposely allow overwriting platformRuntime settings, to create a project later
      // on
      if (!storedExistingProject.platformRuntime
          && updatedProject.platformRuntime
          && !ocUpgradeAllowed) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                "Project: " + updatedProject.projectKey + " cannot be upgraded to openshift usage");
      } else if (storedExistingProject.platformRuntime) {
        // we need to set this, otherwise the provisioning later will not work
        updatedProject.platformRuntime = storedExistingProject.platformRuntime;
      }

      // add (hard) permission data
      if (storedExistingProject.specialPermissionSet) {
        updatedProject.specialPermissionSet = storedExistingProject.specialPermissionSet;
        updatedProject.projectAdminUser = storedExistingProject.projectAdminUser;
        updatedProject.projectAdminGroup = storedExistingProject.projectAdminGroup;
        updatedProject.projectUserGroup = storedExistingProject.projectUserGroup;
        updatedProject.projectReadonlyGroup = storedExistingProject.projectReadonlyGroup;
      }

      updatedProject = createDeliveryChain(updatedProject);

      /*
       * add the already existing data /provisioned/ + we have to add the scm url here, in case we
       * have upgraded a bugtracker only project to a platform project
       */
      storedExistingProject.scmvcsUrl = updatedProject.scmvcsUrl;
      if (updatedProject.quickstarters != null) {
        if (storedExistingProject.quickstarters != null) {
          storedExistingProject.quickstarters.addAll(updatedProject.quickstarters);
        } else {
          storedExistingProject.quickstarters = updatedProject.quickstarters;
        }
      }

      if ((storedExistingProject.repositories != null) && (updatedProject.repositories != null)) {
        storedExistingProject.repositories.putAll(updatedProject.repositories);
      } else if (updatedProject.repositories != null) {
        storedExistingProject.repositories = storedExistingProject.repositories;
      }

      // add the new executions - so people can track what's going on
      storedExistingProject.lastExecutionJobs = updatedProject.lastExecutionJobs;

      // store the updated project
      if (directStorage.updateStoredProject(storedExistingProject)) {
        logger.debug("project {} successfully updated", updatedProject.projectKey);
      }

      // notify user via mail of project updates with embedding links
      mailAdapter.notifyUsersAboutProject(storedExistingProject);

      return ResponseEntity.ok().body(storedExistingProject);
    } catch (Exception exProvision) {
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupResults =
          cleanup(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION, updatedProject);

      String error =
          (cleanupResults.size() == 0)
              ? format(
                  "An error occured while updating project %s, reason %s"
                      + " - but all cleaned up!",
                  updatedProject.projectKey, exProvision.getMessage())
              : format(
                  "An error occured while updating project %s, reason %s"
                      + " - cleanup attempted, but [%s] components are still there!",
                  updatedProject.projectKey, exProvision.getMessage(), cleanupResults);

      logger.error(error);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } finally {
      MDC.remove(STR_LOGFILE_KEY);
    }
  }

  /**
   * Create the delivery chain within the platform in case {@link OpenProjectData#platformRuntime}
   * is set to true
   *
   * @param project the meta information from the API
   * @return the generated, amended Project
   * @throws IOException
   * @throws Exception in case something goes wrong
   */
  private OpenProjectData createDeliveryChain(OpenProjectData project) throws IOException {
    logger.debug(
        "Create delivery chain for: {}, platform? {}, create scm: {}",
        project.projectKey,
        project.platformRuntime,
        (project.scmvcsUrl == null ? true : false));

    if (!project.platformRuntime) {
      return project;
    }

    // create auxilaries - for design and for the ocp artifacts
    String[] auxiliaryRepositories = {"occonfig-artifacts", "design"};

    if (project.scmvcsUrl == null) {
      // create the bugtracker project
      project.scmvcsUrl = bitbucketAdapter.createSCMProjectForODSProject(project);

      Preconditions.checkNotNull(
          project.scmvcsUrl, bitbucketAdapter.getClass() + " did not return scmvcs url");

      project.repositories =
          bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(project, auxiliaryRepositories);

      // provision platform projects
      project = rundeckAdapter.createPlatformProjects(project);
    }

    int newQuickstarters = (project.quickstarters == null ? 0 : project.quickstarters.size());

    int existingComponentRepos = (project.repositories == null ? 0 : project.repositories.size());

    // create repositories dependent of the chosen quickstarters
    Map<String, Map<URL_TYPE, String>> newComponentRepos =
        bitbucketAdapter.createComponentRepositoriesForODSProject(project);
    if (newComponentRepos != null) {
      if (project.repositories != null) {
        project.repositories.putAll(newComponentRepos);
      } else {
        project.repositories = newComponentRepos;
      }
    }

    logger.debug(
        "New quickstarters {}, prior existing repos: {}, now project repos: {}",
        newQuickstarters,
        existingComponentRepos,
        project.repositories.size());

    Preconditions.checkState(
        project.repositories.size() == existingComponentRepos + newQuickstarters,
        format(
            "Class: %s did not create %s new repositories "
                + "for new quickstarters, existing repos: %s",
            bitbucketAdapter.getClass(), newQuickstarters, existingComponentRepos));

    // based on the (changed) repository names, update the
    // quickstarters
    addRepositoryUrlsToQuickstarters(project);

    List<String> auxiliariesToExclude = new ArrayList<>();
    final String projectKey = project.projectKey;
    Arrays.asList(auxiliaryRepositories)
        .forEach(
            repoName ->
                auxiliariesToExclude.add(
                    bitbucketAdapter.createRepoNameFromComponentName(projectKey, repoName)));

    // create jira components from newly created repos
    jiraAdapter.createComponentsForProjectRepositories(project, auxiliariesToExclude);

    // add the long running execution links from the
    // IJobExecutionAdapter, so the consumer can track them
    if (project.lastExecutionJobs == null) {
      project.lastExecutionJobs = new ArrayList<>();
    }
    List<ExecutionsData> jobs = rundeckAdapter.provisionComponentsBasedOnQuickstarters(project);
    logger.debug("New quickstarter rundeck executions: {}", jobs.size());

    for (ExecutionsData singleJob : jobs) {
      project.lastExecutionJobs.add(singleJob.getPermalink());
    }

    return project;
  }

  /**
   * Get a list with all projects in the ODS prov system defined by their key. In this case the
   * quickstarters {@link OpenProjectData#quickstarters} contain also the description of the
   * quickstarter that was used
   *
   * @param id the project's key
   * @return Response with a complete project list of {@link OpenProjectData}
   */
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  public ResponseEntity<OpenProjectData> getProject(@PathVariable String id) {
    OpenProjectData project = filteredStorage.getFilteredSingleProject(id);

    if (project == null) {
      return ResponseEntity.notFound().build();
    }

    if (project.quickstarters != null) {
      List<Map<String, String>> enhancedStarters = new ArrayList<>();

      List<Job> allQuickstarterJobs = rundeckAdapter.getQuickstarters();

      for (Map<String, String> quickstarters : project.quickstarters) {
        String quickstarter = quickstarters.get(OpenProjectData.COMPONENT_TYPE_KEY);
        allQuickstarterJobs.stream()
            .filter(j -> quickstarter.equals(j.getId()))
            .findFirst()
            .ifPresent(
                job -> quickstarters.put(OpenProjectData.COMPONENT_DESC_KEY, job.getDescription()));
        enhancedStarters.add(quickstarters);
      }
      project.quickstarters = enhancedStarters;
    }
    return ResponseEntity.ok(project);
  }

  /**
   * Validate the project name. Duplicates are not allowed in most bugtrackers.
   *
   * @param name the project's name
   * @return Response with HTTP status. If 406 a project with this name exists in JIRA
   */
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
   * OpenProjectData#projectType}
   *
   * @return a list of available template keys
   */
  @RequestMapping(method = RequestMethod.GET, value = "/templates")
  public ResponseEntity<List<String>> getProjectTemplateKeys() {
    return ResponseEntity.ok(projectTemplateKeyNames);
  }

  /**
   * Retrieve the underlying templates from {@link IBugtrackerAdapter} and {@link
   * ICollaborationAdapter}
   *
   * @param key the project type as in {@link OpenProjectData#projectKey}
   * @return a map with the templates (which are implementation specific)
   */
  @RequestMapping(method = RequestMethod.GET, value = "/template/{key}")
  public ResponseEntity<Map<String, String>> getProjectTypeTemplatesForKey(
      @PathVariable String key) {
    Map<String, String> templatesForKey = new HashMap<>();
    logger.debug("retrieving templates for key: " + key);

    Preconditions.checkNotNull(key, "Null template key is not allowed");

    OpenProjectData project = new OpenProjectData();
    project.projectType = key;

    Map<PROJECT_TEMPLATE, String> templates =
        jiraAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    templatesForKey.put(
        "bugTrackerTemplate",
        templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY)
            + "#"
            + templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

    templatesForKey.put(
        "collabSpaceTemplate",
        confluenceAdapter
            .retrieveInternalProjectTypeAndTemplateFromProjectType(project)
            .get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

    return ResponseEntity.ok(templatesForKey);
  }

  /**
   * Validate the project's key name. Duplicates are not allowed in most bugtrackers.
   *
   * @param key the project's name to validate against
   * @return Response with HTTP status. If 406 a project with this key exists in JIRA
   */
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
  @RequestMapping(method = RequestMethod.GET, value = "/key/generate")
  public ResponseEntity<Map<String, String>> generateKey(
      @RequestParam(value = "name") String name) {
    Map<String, String> proj = new HashMap<>();
    proj.put("projectKey", jiraAdapter.buildProjectKey(name));
    return ResponseEntity.ok(proj);
  }

  void shortenDescription(OpenProjectData project) {
    if (project != null && project.description != null && project.description.length() > 100) {
      project.description = project.description.substring(0, 99);
    }
  }

  private void addRepositoryUrlsToQuickstarters(OpenProjectData project) {
    if (project.quickstarters == null || project.repositories == null) {
      logger.debug("Repository Url mgmt - nothing to do on project {}", project.projectKey);
      return;
    }

    for (Map<String, String> option : project.quickstarters) {
      logger.debug("options: " + option);

      String projectComponentKey = option.get(OpenProjectData.COMPONENT_ID_KEY);

      // recreate the repo name that the scmadapter (hopefully)
      // also used
      String repoName =
          bitbucketAdapter.createRepoNameFromComponentName(project.projectKey, projectComponentKey);

      logger.debug(format("Trying to find repo %s in %s", repoName, project.repositories.keySet()));

      Map<URL_TYPE, String> repoUrls = project.repositories.get(repoName);

      if (repoUrls == null) {
        return;
      }

      option.put("git_url_ssh", repoUrls.get(URL_TYPE.URL_CLONE_SSH));
      option.put("git_url_http", repoUrls.get(URL_TYPE.URL_CLONE_HTTP));
    }
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  public ResponseEntity<Object> deleteProject(@PathVariable String id) throws IOException {
    OpenProjectData project = filteredStorage.getFilteredSingleProject(id);

    if (!cleanupAllowed) {
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
              project.projectKey, leftovers);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } else {
      return ResponseEntity.ok().build();
    }
  }

  @RequestMapping(method = RequestMethod.DELETE)
  public ResponseEntity<Object> deleteComponents(@RequestBody OpenProjectData deletableComponents)
      throws IOException {
    if (!cleanupAllowed) {
      throw new IOException("Cleanup of projects is NOT allowed");
    }

    Preconditions.checkNotNull(deletableComponents, "Cannot delete null project");
    Preconditions.checkNotNull(
        deletableComponents.quickstarters, "No quickstarters to delete are passed");

    OpenProjectData project =
        filteredStorage.getFilteredSingleProject(deletableComponents.projectKey);

    if (project == null) {
      return ResponseEntity.notFound().build();
    }

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers =
        cleanup(LIFECYCLE_STAGE.QUICKSTARTER_PROVISION, deletableComponents);

    if (!leftovers.isEmpty()) {
      String error =
          format(
              "Could not delete all components of project " + " %s - leftovers %s",
              project.projectKey, leftovers);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } else {
      return ResponseEntity.ok().build();
    }
  }

  /**
   * In case something breaks during provisioniong, this method is called
   *
   * @param stage the lifecycle stage
   * @param project the project including any created information
   */
  Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {

    if (!cleanupAllowed) {
      return new HashMap<>();
    }

    logger.error("Starting cleanup of project {} in phase {}", project.projectKey, stage);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> notCleanedUpComponents = new HashMap<>();

    notCleanedUpComponents.putAll(rundeckAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(bitbucketAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(confluenceAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(jiraAdapter.cleanup(stage, project));

    notCleanedUpComponents.putAll(filteredStorage.cleanup(stage, project));

    logger.debug(
        "Overall cleanup status of project: {} components left",
        notCleanedUpComponents.size() == 0 ? 0 : notCleanedUpComponents);

    return notCleanedUpComponents;
  }
}
