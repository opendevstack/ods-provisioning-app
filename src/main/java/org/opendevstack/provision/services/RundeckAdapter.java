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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service to interact with rundeck
 *
 * @author Torsten Jaeschke
 */

@Service
public class RundeckAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RundeckAdapter.class);

  @Autowired
  protected RundeckJobStore jobStore;
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

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  private static final String GENERIC_RUNDECK_ERRMSG =  "Error in rundeck call: ";
  
  @Autowired
  RestClient client;

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
        Job job = jobStore.getJob(options.get("component_type"));

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
          ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
          String json = ow.writeValueAsString(execution);
          ExecutionsData data = (ExecutionsData) this.post(url, json, ExecutionsData.class);
          
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

    CrowdUserDetails details = crowdUserDetailsService.loadUserByToken(crowdCookie);

    try {
      List<Job> jobs = getJobs(projectOpenshiftGroup);
      for (Job job : jobs) {
        if (job.getName().equalsIgnoreCase(projectCreateOpenshiftJob)) {
          String url = String.format("%s%s/job/%s/run", rundeckUri, rundeckApiPath, job.getId());
          Execution execution = new Execution();
          Map<String, String> options = new HashMap<>();
          options.put("project_id", project.key.toLowerCase());
          if (details != null) {
            logger.info("project id: " + project.key + " details: " + details);
            options.put("project_admin", details.getUsername());
          } else {
            logger.info("project id: " + project.key + " -- no user found");
          }
          execution.setOptions(options);
          ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
          String json = ow.writeValueAsString(execution);
          ExecutionsData data = (ExecutionsData) this.post(url, json, ExecutionsData.class);

          // add openshift based links - for jenkins we know the link - hence create the direct
          // access link to openshift app domain
          project.openshiftJenkinsUrl =
              "https://" + String.format(projectOpenshiftJenkinsProjectPattern,
                  project.key.toLowerCase(), projectOpenshiftBaseDomain);;

          // we can only add the console based links - as no routes are created per default
          project.openshiftConsoleDevEnvUrl = String.format(projectOpenshiftDevProjectPattern,
              projectOpenshiftConsoleUri, project.key.toLowerCase());

          project.openshiftConsoleTestEnvUrl = String.format(projectOpenshiftTestProjectPattern,
              projectOpenshiftConsoleUri, project.key.toLowerCase());

          project.lastJobs = new ArrayList<String>();
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
  protected void authenticate() throws IOException {
    CrowdUserDetails userDetails =
        (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    String username = userDetails.getUsername();
    String password = manager.getUserPassword();
    
    RequestBody body =
        new FormBody.Builder().add("j_username", username).add("j_password", password).build();
    Request request = new Request.Builder()
        .url(String.format("%s%s/j_security_check", rundeckUri, rundeckSystemPath)).post(body)
        .build();
    Response response = null;
    try 
    {
    	response = client.getClient().newCall(request).execute();
    	if (response.isSuccessful()) {
    		logger.debug("successful rundeck auth");
    	} else {
    		throw new IOException("Could not authenticate: " + username + " : " + response.body());
    	}
    }
    finally {
    	if (response != null)
    		response.close();
    }
  }

  protected Object post(String url, String json, Class clazz) throws IOException {

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Request request =
        new Request.Builder().url(url).post(body).addHeader("Accept", "application/json").build();

    Response response = client.getClient().newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(respBody);
    response.close();
    return new ObjectMapper().readValue(respBody, clazz);
  }

  protected List<Job> getJobs(String group) throws IOException {
    authenticate();
    List<Job> enabledJobs;

    String jobsUrl =
        String.format("%s%s/project/%s/jobs", rundeckUri, rundeckApiPath, rundeckProject);

    HttpUrl.Builder urlBuilder = HttpUrl.parse(jobsUrl).newBuilder();
    urlBuilder.addQueryParameter("groupPath", group);

    Request request = new Request.Builder().url(urlBuilder.build()).get()
        .addHeader("Accept", "application/json").build();

    Response response = client.getClient().newCall(request).execute();

    if (response.isSuccessful()) {
      String respBody = response.body().string();
      logger.debug("ResponseBody: {}", respBody);
      List<Job> jobs = new ObjectMapper().readValue(respBody, new TypeReference<List<Job>>() {});
      enabledJobs = jobs.stream().filter(x -> x.isEnabled()).collect(Collectors.toList());
      jobStore.addJobs(enabledJobs);
    } else {
    	throw new IOException("Error on rundeck call: " + response.body().string());
    }
    response.close();
    return enabledJobs;
  }


}
