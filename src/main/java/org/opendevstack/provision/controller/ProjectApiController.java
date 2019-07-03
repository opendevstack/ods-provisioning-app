/*
 * Copyright 2018 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter.PROJECT_TEMPLATE;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
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
import org.springframework.web.bind.annotation.CookieValue;
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
    CustomAuthenticationManager manager;

    // open for testing
    @Value("${openshift.project.upgrade}")
    boolean ocUpgradeAllowed;

    /**
     * Add a project to JIRA and process subsequent calls to dependent services, to
     * create a complete project stack.
     *
     * @param request
     * @param project
     * @param crowdCookie
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Object> addProject(
            HttpServletRequest request,
            @RequestBody OpenProjectData project,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {

        if (project == null || project.projectKey == null
                || project.projectKey.trim().length() == 0
                || project.projectName == null
                || project.projectName.trim().length() == 0)
        {
            return ResponseEntity.badRequest().body(
                    "Project key and name are mandatory fields to create a project!");
        }

        // fix for opendevstack/ods-provisioning-app/issues/64
        shortenDescription(project);

        project.projectKey = project.projectKey.toUpperCase();
        MDC.put(STR_LOGFILE_KEY, project.projectKey);
        logger.debug("Crowd Cookie: {}", crowdCookie);

        try
        {
            logger.debug("Project to be created: {}",
                    new ObjectMapper().writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(project));

            if (project.specialPermissionSet)
            {
                projectIdentityMgmtAdapter
                        .validateIdSettingsOfProject(project);
            }

            if (project.bugtrackerSpace)
            {
                if (storage.getProject(project.projectKey) != null)
                {
                    {
                        throw new IOException(String.format(
                                "Project with key (%s) already exists",
                                project.projectKey));
                    }
                }

                // create JIRA project
                project = jiraAdapter
                        .createBugtrackerProjectForODSProject(project,
                                crowdCookie);

                // create confluence space
                project = confluenceAdapter
                        .createCollaborationSpaceForODSProject(
                                project, crowdCookie);
                logger.debug("Updated project: {}",
                        new ObjectMapper().writer()
                                .withDefaultPrettyPrinter()
                                .writeValueAsString(project));
            }

            project = createDeliveryChain(project, crowdCookie);

            jiraAdapter.addShortcutsToProject(project, crowdCookie);

            // store project data. The storage is autowired with an interface to enable the
            // option to store data in other data sources
            String filePath = storage.storeProject(project);
            if (filePath != null)
            {
                logger.debug("Project {} successfully stored: {}",
                        project.projectKey, filePath);
            }

            // notify user via mail of project creation with embedding links
            mailAdapter.notifyUsersAboutProject(project);

            // return project data for further processing
            return ResponseEntity.ok().body(project);
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
            client.removeClient(crowdCookie);
            MDC.remove(STR_LOGFILE_KEY);
        }
    }

    /**
     * Add a project to JIRA and process subsequent calls to dependent services, to
     * create a complete project stack.
     *
     * @param request
     * @param project
     * @param crowdCookie
     * @return
     */
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<Object> updateProject(
            HttpServletRequest request,
            @RequestBody OpenProjectData project,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {

        if (project == null
                || project.projectKey.trim().length() == 0)
        {
            return ResponseEntity.badRequest().body(
                    "Project key is mandatory to call update project!");
        }
        MDC.put(STR_LOGFILE_KEY, project.projectKey);

        logger.debug("Update project: " + project.projectKey);
        try
        {
            logger.debug("Project: {}",
                    new ObjectMapper().writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(project));

            OpenProjectData oldProject = storage
                    .getProject(project.projectKey);

            if (oldProject == null)
            {
                return ResponseEntity.notFound().build();
            }

            project.description = oldProject.description;
            project.projectName = oldProject.projectName;
            project.scmvcsUrl = oldProject.scmvcsUrl;
            project.bugtrackerSpace = oldProject.bugtrackerSpace;
            // we purposely allow overwriting openshift settings, to create a project later
            // on
            if (!oldProject.platformRuntime && project.platformRuntime
                    && !ocUpgradeAllowed)
            {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Project: " + project.projectKey
                                + " cannot be upgraded to openshift usage");
            } else if (oldProject.platformRuntime)
            {
                // we need to set this, otherwise the provisioniong will not work
                project.platformRuntime = oldProject.platformRuntime;
            }

            if (oldProject.specialPermissionSet)
            {
                project.specialPermissionSet = oldProject.specialPermissionSet;
                project.projectAdminUser = oldProject.projectAdminUser;
                project.projectAdminGroup = oldProject.projectAdminGroup;
                project.projectUserGroup = oldProject.projectUserGroup;
                project.projectReadonlyGroup = oldProject.projectReadonlyGroup;
            }

            project = createDeliveryChain(project, crowdCookie);

            oldProject.scmvcsUrl = project.scmvcsUrl;
            if (project.quickstarters != null)
            {
                if (oldProject.quickstarters != null)
                {
                    oldProject.quickstarters
                            .addAll(project.quickstarters);
                } else
                {
                    oldProject.quickstarters = project.quickstarters;
                }
            }

            if ((oldProject.repositories != null)
                    && (project.repositories != null))
            {
                oldProject.repositories.putAll(project.repositories);
            }
            // store project data. The storage is autowired with an interface to enable the
            // option to store data in other data sources
            if (storage.updateStoredProject(oldProject))
            {
                logger.debug("project successful updated");
            }
            // notify user via mail of project creation with embedding links
            mailAdapter.notifyUsersAboutProject(oldProject);

            oldProject.lastExecutionJobs = project.lastExecutionJobs;

            /*
             * 
             * return project data for further processing
             */
            return ResponseEntity.ok().body(oldProject);
        } catch (Exception ex)
        {
            logger.error("An error occured while updating project: "
                    + project.projectKey, ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage());
        } finally
        {
            client.removeClient(crowdCookie);
            MDC.remove(STR_LOGFILE_KEY);
        }
    }

    /**
     * Create the bitbucket delivery chain with openshift. OC projects will only be
     * created in case openshiftproject = true
     *
     * @param project
     *            the meta information from the API
     * @param crowdCookie
     *            the authenticated user
     * @return the generated, amended Project
     * @throws IOException
     * @throws Exception
     *             in case something goes wrong
     */
    private OpenProjectData createDeliveryChain(
            OpenProjectData project, String crowdCookie)
            throws IOException
    {
        logger.debug("create delivery chain for:" + project.projectKey
                + " oc?" + project.platformRuntime);

        if (!project.platformRuntime)
        {
            return project;
        }

        if (project.scmvcsUrl == null)
        {
            // create a bitbucket project
            project = bitbucketAdapter.createSCMProjectForODSProject(
                    project, crowdCookie);
            // create auxilaries - for design and for the ocp artifacts
            String[] auxiliaryRepositories = { "occonfig-artifacts",
                    "design" };
            project = bitbucketAdapter
                    .createAuxiliaryRepositoriesForODSProject(project,
                            crowdCookie, auxiliaryRepositories);
            // provision OpenShift project via rundeck
            project = rundeckAdapter.createPlatformProjects(project,
                    crowdCookie);
        }

        // create repositories dependent of the chosen quickstartersers
        project = bitbucketAdapter
                .createComponentRepositoriesForODSProject(project,
                        crowdCookie);

        // create jira components from newly created repos
        jiraAdapter.createComponentsForProjectRepositories(project,
                crowdCookie);

        if (project.lastExecutionJobs == null)
        {
            project.lastExecutionJobs = new ArrayList<>();
        }
        List<ExecutionsData> execs = rundeckAdapter
                .provisionComponentsBasedOnQuickstarters(project);
        for (ExecutionsData exec : execs)
        {
            project.lastExecutionJobs.add(exec.getPermalink());
        }

        return project;
    }

    /**
     * Get a list with all projects in the system defined by the path variable
     *
     * @param request
     * @param crowdCookie
     * @return Response with a complete project list
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public ResponseEntity<OpenProjectData> getProject(
            HttpServletRequest request, @PathVariable String id,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {
        OpenProjectData project = storage.getProject(id);
        if (project == null)
        {
            return ResponseEntity.notFound().build();
        }
        if (project.quickstarters != null)
        {
            List<Map<String, String>> enhancedStarters = new ArrayList<>();
            for (Map<String, String> quickstarterser : project.quickstarters)
            {
                Job job = jobStore.getJob(
                        quickstarterser.get("component_type"));
                if (job != null)
                {
                    quickstarterser.put("component_description",
                            job.getDescription());
                }
                enhancedStarters.add(quickstarterser);
            }
            project.quickstarters = enhancedStarters;
        }
        return ResponseEntity.ok(project);
    }

    /**
     * Validate the project name. Duplicates are not allowed in JIRA.
     * 
     * @param request
     * @param name
     * @param crowdCookie
     * @return Response with HTTP status. If 406 a project with this name exists in
     *         JIRA
     */
    @RequestMapping(method = RequestMethod.GET, value = "/validate")
    public ResponseEntity<Object> validateProject(
            HttpServletRequest request,
            @RequestParam(value = "name") String name,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {
        if (jiraAdapter.projectKeyExists(name, crowdCookie))
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

    @RequestMapping(method = RequestMethod.GET, value = "/templates")
    public ResponseEntity<Object> getProjectTemplateKeys(
            HttpServletRequest request,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {
        return ResponseEntity.ok(projectTemplateKeyNames);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/template/{key}")
    public ResponseEntity<Map<String, String>> getProjectTypeTemplatesForKey(
            HttpServletRequest request, @PathVariable String key)
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
     * Validate the project's key name. Duplicates are not allowed in JIRA.
     * 
     * @param request
     * @param name
     * @param crowdCookie
     * @return Response with HTTP status. If 406 a project with this key exists in
     *         JIRA
     */
    @RequestMapping(method = RequestMethod.GET, value = "/key/validate")
    public ResponseEntity<Object> validateKey(
            HttpServletRequest request,
            @RequestParam(value = "key") String name,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {
        if (jiraAdapter.projectKeyExists(name, crowdCookie))
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
     * TODO make the key pattern dynamic
     *
     * @param request
     * @param name
     * @param crowdCookie
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/key/generate")
    public ResponseEntity<Map<String, String>> generateKey(
            HttpServletRequest request,
            @RequestParam(value = "name") String name,
            @CookieValue(value = "crowd.token_key", required = false) String crowdCookie)
    {
        Map<String, String> proj = new HashMap<>();
        proj.put("key", jiraAdapter.buildProjectKey(name));
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

}
