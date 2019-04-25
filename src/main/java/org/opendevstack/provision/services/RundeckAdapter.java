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

package org.opendevstack.provision.services;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.HttpUrl;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RundeckJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to interact with rundeck
 *
 * @author Torsten Jaeschke
 */

@Service
public class RundeckAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RundeckAdapter.class);

  public static final String COMPONENT_ID_KEY = "component_id";
  public static final String COMPONENT_TYPE_KEY = "component_type";

  @Autowired
  private RundeckJobStore jobStore;

  @Autowired
  private CrowdUserDetailsService crowdUserDetailsService;


  @Value("${rundeck.api.path}")
  private String rundeckApiPath;

  @Value("${rundeck.uri}")
  private String rundeckUri;

  @Value("${rundeck.system.path}")
  private String rundeckSystemPath;

  @Value("${atlassian.domain}")
  private String rundeckDomain;

  @Value("${rundeck.project.name}")
  private String rundeckProject;

  @Value("${rundeck.group.pattern}")
  private String groupPattern;

  @Value("${rundeck.artifact.pattern}")
  private String artifactPattern;

  @Value("${rundeck.project.group.quickstarter}")
  private String projectQuickstarterGroup;

  @Value("${rundeck.project.group.openshift}")
  private String projectOpenshiftGroup;

  @Value("${rundeck.project.openshift.create.name}")
  private String projectCreateOpenshiftJob;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Value("${openshift.console.uri}")
  private String projectOpenshiftConsoleUri;

  @Value("${openshift.test.project.name.pattern}")
  private String projectOpenshiftTestProjectPattern;

  @Value("${openshift.dev.project.name.pattern}")
  private String projectOpenshiftDevProjectPattern;

  @Value("${openshift.jenkins.project.name.pattern}")
  private String projectOpenshiftJenkinsProjectPattern;

  private static final String GENERIC_RUNDECK_ERRMSG =  "Error in rundeck call: ";
  
  @Autowired
  private RestClient client;

  @Autowired
  CustomAuthenticationManager manager;

  public List<Job> getQuickstarter() {
    try {
      return getJobs(projectQuickstarterGroup);
    } catch (IOException ex) {
      logger.error(GENERIC_RUNDECK_ERRMSG, ex);
    }
    return new ArrayList<>();
  }

  public List<ExecutionsData> executeJobs(ProjectData project) throws IOException
  {
    authenticate();
   
    List<ExecutionsData> executionList = new ArrayList<>();
    if (project.quickstart != null) {
      for (Map<String, String> options : project.quickstart) {
        Job job = jobStore.getJob(options.get(COMPONENT_TYPE_KEY));

        String url = String.format("%s%s/job/%s/run", rundeckUri, rundeckApiPath, job.getId());
        String groupId = String.format(groupPattern, project.key.toLowerCase()).replace('_', '-');
        String packageName =
            String.format("%s.%s", String.format(groupPattern, project.key.toLowerCase()),
                options.get("component_id").replace('-', '_'));
        Execution execution = new Execution();

        options.put("group_id", groupId);
        options.put("project_id", project.key.toLowerCase());
        options.put("package_name", packageName);
        execution.setOptions(options);
        try {
            ExecutionsData data = 
        		client.callHttp(url, execution, null, false, RestClient.HTTP_VERB.POST, ExecutionsData.class);

            if (data.getError()) {
        	  throw new IOException ("Could not provision component: " + data.getMessage());
            }
          
          executionList.add(data);
          if (data.getPermalink() != null) {
            options.put("joblink", data.getPermalink());
          }
        } catch (IOException ex) {
          logger.error("Error in running jobs", ex);
        }
      }
    }
    return executionList;
  }

  public ProjectData createOpenshiftProjects(ProjectData project, String crowdCookie)
      throws IOException {

	if (project == null) {
		throw new IOException("Cannot create null project");
	}
	  
    try {
      List<Job> jobs = getJobs(projectOpenshiftGroup);
      for (Job job : jobs) {
        if (job.getName().equalsIgnoreCase(projectCreateOpenshiftJob)) {
          String url = String.format("%s%s/job/%s/run", rundeckUri, rundeckApiPath, job.getId());
          Execution execution = new Execution();
          Map<String, String> options = new HashMap<>();
          options.put("project_id", project.key.toLowerCase());
          if (project.createpermissionset) 
          {
          	  String entitlementGroups =
          		"ADMINGROUP=" + project.adminGroup + "," + 
          		"USERGROUP=" + project.userGroup + "," +
          		"READONLYGROUP=" + project.readonlyGroup;
          	  
              logger.info("project id: " + project.key + 
            	" passed project owner: " + project.admin + 
            	" passed groups: " + entitlementGroups);
              
              options.put("project_admin", project.admin);
              options.put("project_groups", entitlementGroups);
          }
          else 
          {
        	// someone is always logged in :)
        	UserDetails details = crowdUserDetailsService.loadUserByToken(crowdCookie);  
            logger.info("project id: " + project.key + " details: " + details);
            options.put("project_admin", details.getUsername());
          } 
          execution.setOptions(options);
          
          ExecutionsData data = 
        	client.callHttp(url, execution, null, false, RestClient.HTTP_VERB.POST, ExecutionsData.class);

          // add openshift based links - for jenkins we know the link - hence create the direct
          // access link to openshift app domain
          project.openshiftJenkinsUrl =
              "https://" + String.format(projectOpenshiftJenkinsProjectPattern,
                  project.key.toLowerCase(), projectOpenshiftBaseDomain);;

          // we can only add the console based links - as no routes are created per default
          project.openshiftConsoleDevEnvUrl = String.format(projectOpenshiftDevProjectPattern,
              projectOpenshiftConsoleUri.trim(), project.key.toLowerCase());

          project.openshiftConsoleTestEnvUrl = String.format(projectOpenshiftTestProjectPattern,
              projectOpenshiftConsoleUri.trim(), project.key.toLowerCase());

          project.lastJobs = new ArrayList<>();
          project.lastJobs.add(data.getPermalink());
          
          return project;
        }
      }
    } catch (IOException ex) {
      logger.error(GENERIC_RUNDECK_ERRMSG, ex);
      throw ex;
    }
    return project;
  }


  /**
   * method to authenticate against rundeck to store the JSESSIONID in the associated cookiejar
   */
  protected void authenticate() throws IOException 
  {
	  client.callHttpBasicFormAuthenticate(
		  String.format("%s%s/j_security_check", rundeckUri, rundeckSystemPath));
  }

  protected List<Job> getJobs(String group) throws IOException {
    authenticate();
    List<Job> enabledJobs;

    String jobsUrl =
        String.format("%s%s/project/%s/jobs", rundeckUri, rundeckApiPath, rundeckProject);

    HttpUrl.Builder urlBuilder = HttpUrl.parse(jobsUrl).newBuilder();
    urlBuilder.addQueryParameter("groupPath", group);

    List<Job> jobs =
    	client.callHttpTypeRef(urlBuilder.toString(), null, null, false, RestClient.HTTP_VERB.GET,
    		new TypeReference<List<Job>>() {});
    
    enabledJobs = jobs.stream().filter(Job::isEnabled).collect(Collectors.toList());
    jobStore.addJobs(enabledJobs);
    return enabledJobs;
  }

  public String getRundeckAPIPath () {
	  return rundeckUri + rundeckApiPath;
  }
  
}
