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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.IServiceAdapter.PROJECT_TEMPLATE;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RundeckJobStore;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * Rest Controller to handle the process of project creation
 *
 * @author Torsten Jaeschke
 * @author Clemens Utschig
 */
@RestController
@RequestMapping(value = "api/v2/project")
public class ProjectApiController
{
    private static final Logger logger = LoggerFactory
            .getLogger(ProjectApiController.class);

    private static final String STR_LOGFILE_KEY = "loggerFileName";

    @Autowired
    IBugtrackerAdapter jiraAdapter;
    @Autowired
    ICollaborationAdapter confluenceAdapter;
    @Autowired
    private ISCMAdapter bitbucketAdapter;
    @Autowired
    private IJobExecutionAdapter rundeckAdapter;
    @Autowired
    private MailAdapter mailAdapter;
    @Autowired
    private IStorage storage;
    @Autowired
    private RundeckJobStore jobStore;
    @Autowired
    private RestClient client;

    @Autowired
    private IProjectIdentityMgmtAdapter projectIdentityMgmtAdapter;

    @Autowired
    private List<String> projectTemplateKeyNames;

    // open for testing
    @Autowired
    IODSAuthnzAdapter manager;

    // open for testing
    @Value("${openshift.project.upgrade}")
    boolean ocUpgradeAllowed;

    /**
     * Create a new projectand process subsequent calls to dependent services, to
     * create a complete project stack.
     *
     * @param newProject the {@link OpenProjectData} containing the request information
     * @return the created project with additional information, e.g. links or in case
     * an error happens, the error
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Object> addProject(
            @RequestBody OpenProjectData newProject)
    {

        if (newProject == null || newProject.projectKey == null
                || newProject.projectKey.trim().length() == 0
                || newProject.projectName == null
                || newProject.projectName.trim().length() == 0)
        {
            return ResponseEntity.badRequest().body(
                    "Project key and name are mandatory fields to create a project!");
        }

        // fix for opendevstack/ods-provisioning-app/issues/64
        shortenDescription(newProject);

        newProject.projectKey = newProject.projectKey.toUpperCase();
        MDC.put(STR_LOGFILE_KEY, newProject.projectKey);

        try
        {
            logger.debug("Project to be created: {}",
                    new ObjectMapper().writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(newProject));

            if (newProject.specialPermissionSet)
            {
                projectIdentityMgmtAdapter
                        .validateIdSettingsOfProject(newProject);
            }

            if (newProject.bugtrackerSpace)
            {
                // verify the project does NOT exist
                OpenProjectData projectLoad = storage.getProject(newProject.projectKey);
                if (projectLoad != null)
                {
                    {
                        throw new IOException(String.format(
                                "Project with key (%s) already exists: {}",
                                newProject.projectKey, projectLoad.projectKey));
                    }
                }

                // create the bugtracker project
                newProject = jiraAdapter
                        .createBugtrackerProjectForODSProject(newProject);
                
                Preconditions.checkNotNull(newProject.bugtrackerUrl,
                        jiraAdapter.getClass() + 
                        " did not return bugTracker url");
                
                // create confluence space
                newProject.collaborationSpaceUrl = 
                        confluenceAdapter
                        .createCollaborationSpaceForODSProject(
                                newProject);

                Preconditions.checkNotNull(newProject.collaborationSpaceUrl,
                        confluenceAdapter.getClass() + 
                        " did not return collabSpace url");
                
                logger.debug("Updated project: {}",
                        new ObjectMapper().writer()
                                .withDefaultPrettyPrinter()
                                .writeValueAsString(newProject));
            }

            // create the delivery chain, including scm repos, and platform project
            newProject = createDeliveryChain(newProject);

            // add shortcuts into the space
            jiraAdapter.addShortcutsToProject(newProject);

            // store the project data
            String filePath = storage.storeProject(newProject);
            if (filePath != null)
            {
                logger.debug("Project {} successfully stored: {}",
                        newProject.projectKey, filePath);
            }

            // notify user via mail of project creation with embedding links
            mailAdapter.notifyUsersAboutProject(newProject);

            return ResponseEntity.ok().body(newProject);
        } catch (Exception ex)
        {
            logger.error(
                    "An error occured while provisioning project:",
                    ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        } finally
        {
            client.removeClient(manager.getToken());
            MDC.remove(STR_LOGFILE_KEY);
        }
    }

    /**
     * Update a project, e.g. add new quickstarters, 
     * upgrade a bugtracker only project
     * @param updatedProject the project containing the update data
     * @return the updated project
     */
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<Object> updateProject(
            @RequestBody OpenProjectData updatedProject)
    {

        if (updatedProject == null
                || updatedProject.projectKey.trim().length() == 0)
        {
            return ResponseEntity.badRequest().body(
                    "Project key is mandatory to call update project!");
        }
        MDC.put(STR_LOGFILE_KEY, updatedProject.projectKey);

        logger.debug("Update project {}", updatedProject.projectKey);
        try
        {
            logger.debug("Project: {}",
                    new ObjectMapper().writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(updatedProject));

            OpenProjectData storedExistingProject = storage
                    .getProject(updatedProject.projectKey);

            if (storedExistingProject == null)
            {
                return ResponseEntity.notFound().build();
            }

            // add the baseline, to return a full project later
            updatedProject.description = storedExistingProject.description;
            updatedProject.projectName = storedExistingProject.projectName;
            
            // add the scm url & bugtracker space bool
            updatedProject.scmvcsUrl = storedExistingProject.scmvcsUrl;
            updatedProject.bugtrackerSpace = storedExistingProject.bugtrackerSpace;
            // we purposely allow overwriting platformRuntime settings, to create a project later
            // on
            if (!storedExistingProject.platformRuntime && updatedProject.platformRuntime
                    && !ocUpgradeAllowed)
            {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Project: " + updatedProject.projectKey
                                + " cannot be upgraded to openshift usage");
            } else if (storedExistingProject.platformRuntime)
            {
                // we need to set this, otherwise the provisioning later will not work
                updatedProject.platformRuntime = storedExistingProject.platformRuntime;
            }

            // add (hard) permission data
            if (storedExistingProject.specialPermissionSet)
            {
                updatedProject.specialPermissionSet = storedExistingProject.specialPermissionSet;
                updatedProject.projectAdminUser = storedExistingProject.projectAdminUser;
                updatedProject.projectAdminGroup = storedExistingProject.projectAdminGroup;
                updatedProject.projectUserGroup = storedExistingProject.projectUserGroup;
                updatedProject.projectReadonlyGroup = storedExistingProject.projectReadonlyGroup;
            }

            updatedProject = createDeliveryChain(updatedProject);

            /*
             * add the already existing data /provisioned/ 
             * + we have to add the scm url here, in case we have upgraded 
             * a bugtracker only project to a platform project
             */
            storedExistingProject.scmvcsUrl = updatedProject.scmvcsUrl;
            if (updatedProject.quickstarters != null)
            {
                if (storedExistingProject.quickstarters != null)
                {
                    storedExistingProject.quickstarters
                            .addAll(updatedProject.quickstarters);
                } else
                {
                    storedExistingProject.quickstarters = updatedProject.quickstarters;
                }
            }

            if ((storedExistingProject.repositories != null)
                    && (updatedProject.repositories != null))
            {
                storedExistingProject.repositories.putAll(updatedProject.repositories);
            }
            
            // store the updated project
            if (storage.updateStoredProject(storedExistingProject))
            {
                logger.debug("project {} successfully updated", updatedProject.projectKey);
            }
            
            // notify user via mail of project updates with embedding links
            mailAdapter.notifyUsersAboutProject(storedExistingProject);

            // add the executions - so people can track what's going on
            storedExistingProject.lastExecutionJobs = updatedProject.lastExecutionJobs;
            return ResponseEntity.ok().body(storedExistingProject);
        } catch (Exception ex)
        {
            logger.error("An error occured while updating project: "
                    + updatedProject.projectKey, ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        } finally
        {
            client.removeClient(manager.getToken());
            MDC.remove(STR_LOGFILE_KEY);
        }
    }

    /**
     * Create the delivery chain within the platform in case
     * {@link OpenProjectData#platformRuntime} is set to true
     *
     * @param project
     *            the meta information from the API
     * @return the generated, amended Project
     * @throws IOException
     * @throws Exception
     *             in case something goes wrong
     */
    private OpenProjectData createDeliveryChain(
            OpenProjectData project)
            throws IOException
    {
        logger.debug("Create delivery chain for: {}, platform? {}, create scm: {}",
                project.projectKey, project.platformRuntime, 
                (project.scmvcsUrl == null ? true : false));

        if (!project.platformRuntime)
        {
            return project;
        }

        if (project.scmvcsUrl == null)
        {
            // create the bugtracker project
            project.scmvcsUrl = 
                bitbucketAdapter.createSCMProjectForODSProject(
                    project);
            
            Preconditions.checkNotNull(project.scmvcsUrl,
                    bitbucketAdapter.getClass() + 
                    " did not return scmvcs url");
            
            // create auxilaries - for design and for the ocp artifacts
            String[] auxiliaryRepositories = 
                { "occonfig-artifacts", "design" };
            
            project.repositories = 
                    bitbucketAdapter
                    .createAuxiliaryRepositoriesForODSProject(project,
                            auxiliaryRepositories);
            
            // provision platform projects
            project = rundeckAdapter.createPlatformProjects(project);
        }
        
        int newQuickstarters = 
                (project.quickstarters == null ? 
                        0 : project.quickstarters.size());
        
        int existingComponentRepos =
                (project.repositories == null ?
                        0 : project.repositories.size());
        
        // create repositories dependent of the chosen quickstarters
        project.repositories = bitbucketAdapter
                .createComponentRepositoriesForODSProject(project);
        
        logger.debug("New quickstarters {}, existing repos: {}",
                newQuickstarters, existingComponentRepos);
        
        Preconditions.checkState(
                project.repositories.size() == 
                existingComponentRepos + newQuickstarters,
                String.format("Class: %s did not create repositories for %s new quickstarters", 
                        bitbucketAdapter.getClass(), newQuickstarters));
        
        // based on the (changed) repository names, update the 
        // quickstarters
        addRepositoryUrlsToQuickstarters(project);

        // create jira components from newly created repos
        jiraAdapter.createComponentsForProjectRepositories(project);

        // add the long running execution links from the 
        // IJobExecutionAdapter, so the consumer can track them
        if (project.lastExecutionJobs == null)
        {
            project.lastExecutionJobs = new ArrayList<>();
        }
        List<ExecutionsData> jobs = rundeckAdapter
                .provisionComponentsBasedOnQuickstarters(project);
        for (ExecutionsData singleJob : jobs)
        {
            project.lastExecutionJobs.add(singleJob.getPermalink());
        }

        return project;
    }

    /**
     * Get a list with all projects in the ODS prov system defined by their key.
     * In this case the quickstarters {@link OpenProjectData#quickstarters}
     * contain also the description of the quickstarter that was used
     * @param id the project's key
     * @return Response with a complete project list of {@link OpenProjectData}
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public ResponseEntity<OpenProjectData> getProject(
            @PathVariable String id)
    {
        OpenProjectData project = storage.getProject(id);
        if (project == null)
        {
            return ResponseEntity.notFound().build();
        }
        if (project.quickstarters != null)
        {
            List<Map<String, String>> enhancedStarters = new ArrayList<>();
            for (Map<String, String> quickstarters : project.quickstarters)
            {
                Job job = jobStore.getJob(
                        quickstarters.get(OpenProjectData.COMPONENT_TYPE_KEY));
                if (job != null)
                {
                    quickstarters.put(
                            OpenProjectData.COMPONENT_DESC_KEY,
                            job.getDescription());
                }
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
     * @return Response with HTTP status. If 406 a project with this name exists in
     *         JIRA
     */
    @RequestMapping(method = RequestMethod.GET, value = "/validate")
    public ResponseEntity<Object> validateProject(
            @RequestParam(value = "projectName") String name)
    {
        if (jiraAdapter.projectKeyExists(name))
        {
            HashMap<String, Object> result = new HashMap<>();
            result.put("error", true);
            result.put("error_message",
                    "A project with this name exists");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(result);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Get all available (project) template keys, which can be used later
     * in {@link OpenProjectData#projectType}
     * @return a list of available template keys
     */
    @RequestMapping(method = RequestMethod.GET, value = "/templates")
    public ResponseEntity<List<String>> getProjectTemplateKeys()
    {
        return ResponseEntity.ok(projectTemplateKeyNames);
    }

    /**
     * Retrieve the underlying templates from {@link IBugtrackerAdapter} and
     * {@link ICollaborationAdapter}
     * @param key the project type as in {@link OpenProjectData#projectKey}
     * @return a map with the templates (which are implementation specific)
     */
    @RequestMapping(method = RequestMethod.GET, value = "/template/{key}")
    public ResponseEntity<Map<String, String>> getProjectTypeTemplatesForKey(
            @PathVariable String key)
    {
        Map<String, String> templatesForKey = new HashMap<>();
        logger.debug("retrieving templates for key: " + key);

        Preconditions.checkNotNull(key,
                "Null template key is not allowed");

        OpenProjectData project = new OpenProjectData();
        project.projectType = key;

        Map<PROJECT_TEMPLATE, String> templates = jiraAdapter
                .retrieveInternalProjectTypeAndTemplateFromProjectType(
                        project);

        templatesForKey.put("bugTrackerTemplate", templates.get(
                IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY)
                + "#" + templates.get(
                        IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

        templatesForKey.put("collabSpaceTemplate", confluenceAdapter
                .retrieveInternalProjectTypeAndTemplateFromProjectType(
                        project)
                .get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

        return ResponseEntity.ok(templatesForKey);
    }

    /**
     * Validate the project's key name. Duplicates are not allowed in most 
     * bugtrackers.
     * 
     * @param name the project's name to validate against
     * @return Response with HTTP status. If 406 a project with this key exists in
     *         JIRA
     */
    @RequestMapping(method = RequestMethod.GET, value = "/key/validate")
    public ResponseEntity<Object> validateKey(
            @RequestParam(value = "projectKey") String key)
    {
        if (jiraAdapter.projectKeyExists(key))
        {
            HashMap<String, Object> result = new HashMap<>();
            result.put("error", true);
            result.put("error_message",
                    "A key with this name exists");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(result);
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
            @RequestParam(value = "name") String name)
    {
        Map<String, String> proj = new HashMap<>();
        proj.put("projectKey", jiraAdapter.buildProjectKey(name));
        return ResponseEntity.ok(proj);
    }

    void shortenDescription(OpenProjectData project)
    {
        if (project != null && project.description != null
                && project.description.length() > 100)
        {
            project.description = project.description.substring(0,
                    99);
        }
    }
    
    private void addRepositoryUrlsToQuickstarters
        (OpenProjectData project)
    {
        if (project.quickstarters == null || project.repositories == null) 
        {
            return;
        }
        
        for (Map<String, String> option : project.quickstarters)
        {
            String projectComponentKey =
                    option.get(OpenProjectData.COMPONENT_ID_KEY);
            
            // recreate the repo name that the scmadapter (hopefully)
            // also used
            String repoName = 
                    bitbucketAdapter.createRepoNameFromComponentName
                    (project.projectKey, projectComponentKey);
            
            logger.debug(String.format(
                    "Trying to find repo %s in %s", repoName, 
                    project.repositories.keySet()));

            Map<URL_TYPE, String> repoUrls = 
                    project.repositories.get(repoName);
            
            if (repoUrls == null) 
            {
                return;
            }
                        
            option.put("git_url_ssh", repoUrls.get(URL_TYPE.URL_CLONE_SSH));
            option.put("git_url_http", repoUrls.get(URL_TYPE.URL_CLONE_HTTP));
        }
    }
    
}
