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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.RundeckJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
 */
@RestController
@RequestMapping(value = "api/v1/project")
public class ProjectApiController {

  private static final Logger logger = LoggerFactory.getLogger(ProjectApiController.class);

  @Autowired
  private JiraAdapter jiraAdapter;
  @Autowired
  private ConfluenceAdapter confluenceAdapter;
  @Autowired
  private BitbucketAdapter bitbucketAdapter;
  @Autowired
  private RundeckAdapter rundeckAdapter;
  @Autowired
  private MailAdapter mailAdapter;
  @Autowired
  private IStorage storage;
  @Autowired
  private RundeckJobStore jobStore;

  /**
   * Add a project to JIRA and process subsequent calls to dependent services, to create a complete
   * project stack.
   *
   * @param request
   * @param project
   * @param crowdCookie
   * @return
   */
  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<Object> addProject(HttpServletRequest request, @RequestBody ProjectData project,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {

    logger.debug("Crowd Cookie: {}", crowdCookie);
    try {
      logger.debug("Project: {}",
          new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(project));

            if (project.jiraconfluencespace)
            {
            	if (storage.listProjectHistory().containsKey(project.key))
            	{
            		throw new IOException("Project with key " + project.key + " already exists");
            	}

	            //create JIRA project
	            project = jiraAdapter.createJiraProjectForProject(project, crowdCookie);

	            //create confluence space
	            project = confluenceAdapter.createConfluenceSpaceForProject(project, crowdCookie);
	            logger.debug("Updated project: {}", new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(project));
            }

      project = createDeliveryChain(project, crowdCookie, false);
      // clear admin user list for storage to prevent serialization problems
      project.admins = new ArrayList<>();

      // store project data. The storage is autowired with an interface to enable the
      // option to store data in other data sources
      String filePath = storage.storeProject(project);
      if (filePath != null) {
        logger.debug("project successful stored: {}", filePath);
      }
      // notify user via mail of project creation with embedding links
      try {
        mailAdapter.notifyUsersAboutProject(project);
      } catch (Exception ex) {
        logger.error("Can't send mail: {}", ex);
      }

      // return project data for further processing
      return ResponseEntity.ok().body(project);
    } catch (Exception ex) {
      logger.error("An error occured: {}", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
  }

  /**
   * Add a project to JIRA and process subsequent calls to dependent services, to create a complete
   * project stack.
   *
   * @param request
   * @param project
   * @param crowdCookie
   * @return
   */
  @RequestMapping(method = RequestMethod.PUT)
  public ResponseEntity<Object> updateProject(HttpServletRequest request, @RequestBody ProjectData project,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {

    logger.debug("Update project");
    try {
      logger.debug("Project: {}",
        new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(project));

	  ProjectData oldProject = storage.getProject(project.key);
      project.description = oldProject.description;
      project.name = oldProject.name;
      project.bitbucketUrl = oldProject.bitbucketUrl;

      project = createDeliveryChain(project, crowdCookie, true);

      oldProject.bitbucketUrl = project.bitbucketUrl;
      if (project.quickstart != null) {
        if (oldProject.quickstart != null) {
          oldProject.quickstart.addAll(project.quickstart);
        } else {
          oldProject.quickstart = project.quickstart;
        }
      }

      if (oldProject.repositories != null) {
        if (project.repositories != null) {
          oldProject.repositories.putAll(project.repositories);
        }
      }
      // store project data. The storage is autowired with an interface to enable the
      // option to store data in other data sources
      if (storage.updateStoredProject(oldProject)) {
        logger.debug("project successful updated");
      }
      // notify user via mail of project creation with embedding links
      mailAdapter.notifyUsersAboutProject(oldProject);

      /*
       * 
       * return project data for further processing
       */
      return ResponseEntity.ok().body(oldProject);
    } catch (Exception ex) {
      logger.error("An error occured: {}", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
  }

  /**
   * Create the bitbucket delivery chain with openshift
   *
   * @param project
   * @param crowdCookie
   * @return
   * @throws Exception
   */
  private ProjectData createDeliveryChain(ProjectData project, String crowdCookie, boolean update)
      throws Exception {

    logger.debug("create delivery chain");

    if (project.bitbucketUrl == null) {
      // create a bitbucket project
      project = bitbucketAdapter.createBitbucketProjectsForProject(project, crowdCookie);
      // provision OpenShift project via rundeck
      project = rundeckAdapter.createOpenshiftProjects(project, crowdCookie);
    }

    // create repositories dependent of the chosen quickstarters
    project = bitbucketAdapter.createRepositoriesForProject(project, crowdCookie);

    if (!update) {
      String[] auxiliaryRepositories = {"occonfig-artifacts", "design"};
      project = bitbucketAdapter.createAuxiliaryRepositoriesForProject(project, crowdCookie,
          auxiliaryRepositories);
    }
    rundeckAdapter.executeJobs(project);

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
  public ResponseEntity<ProjectData> getProject(HttpServletRequest request, @PathVariable String id,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    ProjectData project = storage.getProject(id);
    if (project.quickstart != null) {
      List<Map<String, String>> enhancedStarters = new ArrayList<>();
      for (Map<String, String> quickstarter : project.quickstart) {
        Job job = jobStore.getJob(quickstarter.get("component_type"));
        if (job != null) {
          quickstarter.put("component_description", job.getDescription());
        }
        enhancedStarters.add(quickstarter);
      }
      project.quickstart = enhancedStarters;
    }
    return ResponseEntity.ok(project);
  }

  /**
   * Get a list with all projects in the system defined by the path variable
   *
   * @param request
   * @param crowdCookie
   * @return Response with a complete project list
   */
  @RequestMapping(method = RequestMethod.GET, value = "/{system}/all")
  public ResponseEntity<List<FullJiraProject>> getProjects(HttpServletRequest request, @PathVariable String system,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    switch (system) {
      case "jira":
      case "confluence":
      case "bitbucket":
      default:
        return ResponseEntity.ok(jiraAdapter.getProjects(crowdCookie, null));
    }
  }

  /**
   * Validate the project name. Duplicates are not allowed in JIRA.
   * 
   * @param request
   * @param name
   * @param crowdCookie
   * @return Response with HTTP status. If 406 a project with this name exists in JIRA
   */
  @RequestMapping(method = RequestMethod.GET, value = "/validate")
  public ResponseEntity<Object> validateProject(HttpServletRequest request,
      @RequestParam(value = "name") String name,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    if (jiraAdapter.keyExists(name, crowdCookie)) {
      HashMap<String, Object> result = new HashMap<>();
      result.put("error", true);
      result.put("error_message", "A project with this name exists");
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(result);
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Validate the project's key name. Duplicates are not allowed in JIRA.
   * 
   * @param request
   * @param name
   * @param crowdCookie
   * @return Response with HTTP status. If 406 a project with this key exists in JIRA
   */
  @RequestMapping(method = RequestMethod.GET, value = "/key/validate")
  public ResponseEntity<Object> validateKey(HttpServletRequest request,
      @RequestParam(value = "key") String name,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    if (jiraAdapter.keyExists(name, crowdCookie)) {
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
   * TODO make the key pattern dynamic
   *
   * @param request
   * @param name
   * @param crowdCookie
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/key/generate")
  public ResponseEntity<Map<String, String>> generateKey(HttpServletRequest request,
      @RequestParam(value = "name") String name,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    Map<String, String> proj = new HashMap<>();
    proj.put("key", jiraAdapter.buildProjectKey(name));
    return ResponseEntity.ok(proj);
  }
}
