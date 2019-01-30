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

import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.RundeckAdapter;
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

/**
 * Rest Controller to handle the process of project creation
 *
 * @author Torsten Jaeschke
 * @author Clemens Utschig
 */
@RestController
@RequestMapping(value = "api/v1/project")
public class ProjectApiController {

  private static final Logger logger = LoggerFactory.getLogger(ProjectApiController.class);

  private static final String STR_LOGFILE_KEY = "loggerFileName";
  
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

	if (project == null || project.key == null || project.key.trim().length() == 0 || 
		project.name == null || project.name.trim().length() == 0) 
	{
	  return ResponseEntity.badRequest().body("Project key and name are mandatory fields to create a project!");
	}
	  
	project.key = project.key.toUpperCase();
	MDC.put(STR_LOGFILE_KEY, project.key);
    logger.debug("Crowd Cookie: {}", crowdCookie);
    
    try {
        logger.debug("Project: {}",
          new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(project));

      	if (project.createpermissionset) {
      		projectIdentityMgmtAdapter.validateIdSettingsOfProject(project);
      	}
      
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
	    
	    project = createDeliveryChain(project, crowdCookie);

	    // clear admin user list for storage to prevent serialization problems
	    project.admins = new ArrayList<>();

	    jiraAdapter.addShortcutsToProject(project, crowdCookie);
	    
      // store project data. The storage is autowired with an interface to enable the
      // option to store data in other data sources
      String filePath = storage.storeProject(project);
      if (filePath != null) {
        logger.debug("project successful stored: {}", filePath);
      }
      
      // notify user via mail of project creation with embedding links
      mailAdapter.notifyUsersAboutProject(project);
      
      // return project data for further processing
      return ResponseEntity.ok().body(project);
    } catch (Exception ex) {
      logger.error("An error occured while provisioning project: {}", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    } finally {
    	client.removeClient(crowdCookie);
    	MDC.remove(STR_LOGFILE_KEY);
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

	if (project == null || project.key.trim().length() == 0) {
		  return ResponseEntity.badRequest().body("Project key is mandatory to call update project!");
	}
	MDC.put(STR_LOGFILE_KEY, project.key);
	  
    logger.debug("Update project: " + project.key);
    try {
      logger.debug("Project: {}",
        new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(project));

	  ProjectData oldProject = storage.getProject(project.key);
	  
	  if (oldProject == null) {
		  return ResponseEntity.notFound().build();
	  }
	  
      project.description = oldProject.description;
      project.name = oldProject.name;
      project.bitbucketUrl = oldProject.bitbucketUrl;
      project.jiraconfluencespace = oldProject.jiraconfluencespace;
      // we purposely allow overwriting openshift settings, to create a project later on
      if (!oldProject.openshiftproject && project.openshiftproject &&
    		 !ocUpgradeAllowed) 
      {
    	  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
    	  	body("Project: " + project.key + " cannot be upgraded to openshift usage");
      } else if (oldProject.openshiftproject) 
      {
    	  // we need to set this, otherwise the provisioniong will not work
    	  project.openshiftproject = oldProject.openshiftproject;
      }
      
      if (oldProject.createpermissionset) 
      {
    	  project.createpermissionset = oldProject.createpermissionset;
    	  project.admin = oldProject.admin;
    	  project.adminGroup = oldProject.adminGroup;
    	  project.userGroup = oldProject.userGroup;
    	  project.readonlyGroup = oldProject.readonlyGroup;
      }

      project = createDeliveryChain(project, crowdCookie);

      oldProject.bitbucketUrl = project.bitbucketUrl;
      if (project.quickstart != null) {
        if (oldProject.quickstart != null) {
          oldProject.quickstart.addAll(project.quickstart);
        } else {
          oldProject.quickstart = project.quickstart;
        }
      }

      if ((oldProject.repositories != null) && (project.repositories != null)) 
      {
          oldProject.repositories.putAll(project.repositories);
      }
      // store project data. The storage is autowired with an interface to enable the
      // option to store data in other data sources
      if (storage.updateStoredProject(oldProject)) {
        logger.debug("project successful updated");
      }
      // notify user via mail of project creation with embedding links
      mailAdapter.notifyUsersAboutProject(oldProject);

      oldProject.lastJobs = project.lastJobs;
      
      /*
       * 
       * return project data for further processing
       */
      return ResponseEntity.ok().body(oldProject);
    } catch (Exception ex) {
      logger.error("An error occured while updating project: " + project.key, ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    } finally {
    	client.removeClient(crowdCookie);
    	MDC.remove(STR_LOGFILE_KEY);
    }
  }

  /**
   * Create the bitbucket delivery chain with openshift. OC projects will only be created in case
   * openshiftproject = true
   *
   * @param project the meta information from the API
   * @param crowdCookie the authenticated user
   * @return the generated, amended Project
   * @throws IOException 
   * @throws Exception in case something goes wrong
   */
  private ProjectData createDeliveryChain(ProjectData project, String crowdCookie)
    throws IOException
  {
    logger.debug("create delivery chain for:" + project.key + " oc?" + project.openshiftproject);

    if (!project.openshiftproject) {
    	return project;
    }
    
    if (project.bitbucketUrl == null) 
    {
      // create a bitbucket project
      project = bitbucketAdapter.createBitbucketProjectsForProject(project, crowdCookie);
      // create auxilaries - for design and for the ocp artifacts
      String[] auxiliaryRepositories = {"occonfig-artifacts", "design"};
      project = bitbucketAdapter.createAuxiliaryRepositoriesForProject(project, crowdCookie,
          auxiliaryRepositories);
      // provision OpenShift project via rundeck
      project = rundeckAdapter.createOpenshiftProjects(project, crowdCookie);
    }

    // create repositories dependent of the chosen quickstarters
    project = bitbucketAdapter.createRepositoriesForProject(project, crowdCookie);

    if (project.lastJobs == null) {
    	project.lastJobs = new ArrayList<String>();
    }
    List<ExecutionsData> execs = rundeckAdapter.executeJobs(project);
    for (ExecutionsData exec : execs) {
    	project.lastJobs.add(exec.getPermalink());
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
  public ResponseEntity<ProjectData> getProject(HttpServletRequest request, @PathVariable String id,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) {
    ProjectData project = storage.getProject(id);
    if (project == null) {
    	return ResponseEntity.notFound().build();
    }
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

  @RequestMapping(method = RequestMethod.GET, value = "/templates")
  public ResponseEntity<Object> getProjectTemplateKeys(HttpServletRequest request,
      @CookieValue(value = "crowd.token_key", required = false) String crowdCookie) 
  {
    return ResponseEntity.ok(projectTemplateKeyNames);
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
