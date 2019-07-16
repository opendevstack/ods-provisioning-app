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

package org.opendevstack.provision.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RundeckJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class RundeckAdapter implements IJobExecutionAdapter {

  private static final Logger logger = LoggerFactory.getLogger(RundeckAdapter.class);

  public static final String DELETE_COMPONENT_JOB = "delete-component";
  
  public static final String DELETE_PROJECTS_JOB = "delete-projects";
  
  @Autowired
  private RundeckJobStore jobStore;

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

  private static final String GENERIC_RUNDECK_ERRMSG = "Error in rundeck call: ";

  @Autowired
  private RestClient client;

  @Autowired
  IODSAuthnzAdapter manager;

  public List<Job> getQuickstarters() {
    try {
      return getJobs(projectQuickstarterGroup);
    } catch (IOException ex) {
      logger.error(GENERIC_RUNDECK_ERRMSG, ex);
    }
    return new ArrayList<>();
  }

  public List<ExecutionsData> provisionComponentsBasedOnQuickstarters(OpenProjectData project)
      throws IOException {
    if (project == null || project.quickstarters == null) {
      return new ArrayList<>();
    }

    authenticate();

    List<ExecutionsData> executionList = new ArrayList<>();

    if (jobStore.size() == 0) {
      jobStore.addJobs(getJobs(projectOpenshiftGroup));
    }

    if (project.quickstarters != null) {
      for (Map<String, String> options : project.quickstarters) {
        String jobId = options.get(OpenProjectData.COMPONENT_TYPE_KEY);
        String quickstarterName = options.get(OpenProjectData.COMPONENT_ID_KEY);
        String groupId =
            String.format(groupPattern, project.projectKey.toLowerCase()).replace('_', '-');
        String packageName =
            String.format("%s.%s", String.format(groupPattern, project.projectKey.toLowerCase()),
                options.get(OpenProjectData.COMPONENT_ID_KEY).replace('-', '_'));

        options.put("group_id", groupId);
        options.put("project_id", project.projectKey.toLowerCase());
        options.put("package_name", packageName);

        executionList.add(prepareAndExecuteJob(jobId, options));
      }
    }
    return executionList;
  }

  @Override
  public OpenProjectData createPlatformProjects(OpenProjectData project) throws IOException {

    Preconditions.checkNotNull(project, "Cannot create null project");

    try {
      List<Job> jobs = getJobs(projectOpenshiftGroup);
      Map<String, String> options = new HashMap<>();
      options.put("project_id", project.projectKey.toLowerCase());
      if (project.specialPermissionSet) {
        String entitlementGroups = "ADMINGROUP=" + project.projectAdminGroup + "," + "USERGROUP="
            + project.projectUserGroup + "," + "READONLYGROUP=" + project.projectReadonlyGroup;

        logger.debug("project id: {} passed project owner: {} passed groups: {}",
            project.projectKey, project.projectAdminUser, entitlementGroups);

        options.put("project_admin", project.projectAdminUser);
        options.put("project_groups", entitlementGroups);
      } else {
        // someone is always logged in :)
        logger.debug("project id: {} admin: {}", project.projectKey, manager.getUserName());
        options.put("project_admin", manager.getUserName());
      }
      ExecutionsData data = prepareAndExecuteJob(projectCreateOpenshiftJob, options);

      // add openshift based links - for jenkins we know the link - hence create the
      // direct
      // access link to openshift app domain
      project.platformBuildEngineUrl =
          "https://" + String.format(projectOpenshiftJenkinsProjectPattern,
              project.projectKey.toLowerCase(), projectOpenshiftBaseDomain);

      // we can only add the console based links - as no routes are created per
      // default
      project.platformDevEnvironmentUrl = String.format(projectOpenshiftDevProjectPattern,
          projectOpenshiftConsoleUri.trim(), project.projectKey.toLowerCase());

      project.platformTestEnvironmentUrl = String.format(projectOpenshiftTestProjectPattern,
          projectOpenshiftConsoleUri.trim(), project.projectKey.toLowerCase());

      project.lastExecutionJobs = new ArrayList<>();
      project.lastExecutionJobs.add(data.getPermalink());

      return project;
    } catch (IOException ex) {
      String error = String.format("Cannot execute job for project %s, error: %s",
          project.projectKey, ex.getMessage());
      logger.error(error, ex);
      throw ex;
    }
  }

  /**
   * method to authenticate against rundeck to store the JSESSIONID in the associated cookiejar
   */
  protected void authenticate() throws IOException {
    client.callHttpBasicFormAuthenticate(
        String.format("%s%s/j_security_check", rundeckUri, rundeckSystemPath));
  }

  protected List<Job> getJobs(String group) throws IOException {
    authenticate();
    List<Job> enabledJobs;

    String jobsUrl =
        String.format("%s%s/project/%s/jobs", rundeckUri, rundeckApiPath, rundeckProject);

    Map<String, String> jobPath = new HashMap<>();
    jobPath.put("groupPath", group);

    List<Job> jobs = client.callHttpTypeRef(jobsUrl, jobPath, false, RestClient.HTTP_VERB.GET,
        new TypeReference<List<Job>>() {});

    enabledJobs = jobs.stream().filter(Job::isEnabled).collect(Collectors.toList());
    jobStore.addJobs(enabledJobs);
    return enabledJobs;
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException();
  }

  @Override
  public String getAdapterApiUri() {
    return rundeckUri + rundeckApiPath;
  }

  private ExecutionsData prepareAndExecuteJob(String jobNameOrId, Map<String, String> options)
      throws IOException {
    Preconditions.checkNotNull(jobNameOrId, "Cannot execute Null Job!");

    Job job = jobStore.getJob(jobNameOrId);

    if (job == null) {
      String jobByName = jobStore.getJobIdForJobName(jobNameOrId);
      job = jobStore.getJob(jobByName);
    }

    if (job == null) {
      throw new IOException(
          String.format("Cannot find job with id: %s, jobs: %s", jobNameOrId, jobStore.toString()));
    }

    String url = String.format("%s%s/job/%s/run", rundeckUri, rundeckApiPath, job.getId());

    Execution execution = new Execution();
    if (options != null) {
      execution.setOptions(options);
    }
    try {
      ExecutionsData data =
          client.callHttp(url, execution, false, RestClient.HTTP_VERB.POST, ExecutionsData.class);

      if (data != null && data.getError()) {
        throw new IOException(data.getMessage());
      }
      return data;
    } catch (IOException rundeckException) {
      logger.error("Error starting job {} - details:", jobNameOrId, rundeckException);
      throw rundeckException;
    }
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(LIFECYCLE_STAGE stage,
      OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    if (stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION)) {
      if (project.lastExecutionJobs != null && !project.lastExecutionJobs.isEmpty()) {
        String deleteProjectJob = jobStore.getJobIdForJobName(DELETE_PROJECTS_JOB);
        if (deleteProjectJob == null) {
          logger.error("Cannot find delete-projects job, hence" + " cannot delete project!");
          leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
        } else {
          logger.debug("Calling delete-projects job for project {}" + " with id {}",
              project.projectKey, deleteProjectJob);

          Map<String, String> options = new HashMap<>();
          options.put("project_id", project.projectKey.toLowerCase());

          try {
            prepareAndExecuteJob(deleteProjectJob, options);
          } catch (Exception allExecExceptions) {
            logger.debug("Could not start delete job for project {}, {}", project.projectKey,
                allExecExceptions.getMessage());
            leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
            return leftovers;
          }
          return leftovers;
        }
        return leftovers;
      } else {
        logger.debug("Project {} not affected from cleanup", project.projectKey);
        return leftovers;

      }
    }

    if (project.quickstarters != null || project.quickstarters.size() > 0) {
      String deleteComponentJob = jobStore.getJobIdForJobName(DELETE_COMPONENT_JOB);

      int nonDeletedQuickstarters = 0;
      List<String> quickstartersToDelete = new ArrayList<>();
      for (Map<String, String> quickstarterOptions : project.quickstarters) {
        quickstartersToDelete.add(quickstarterOptions.get(OpenProjectData.COMPONENT_ID_KEY));
      }

      logger.debug("Cleanup of quickstarters {}", quickstartersToDelete);
      for (String quickstarterName : quickstartersToDelete) {
        logger.debug("Cleanup of quickstarter {} thru job {}", quickstarterName,
            deleteComponentJob);

        Map<String, String> options = new HashMap<>();
        options.put("project_id", project.projectKey.toLowerCase());
        options.put("component_id", quickstarterName);
        try {
          prepareAndExecuteJob(deleteComponentJob, options);
        } catch (Exception allExecExceptions) {
          logger.debug("Could not start delete job for component {}, {}", quickstarterName,
              allExecExceptions.getMessage());
          nonDeletedQuickstarters++;
        }
      }

      if (nonDeletedQuickstarters > 0) {
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER, nonDeletedQuickstarters);
      }
    }

    logger.debug("Cleanup done - status: {} components are left ..",
        leftovers.size() == 0 ? 0 : leftovers.size());

    return leftovers;
  }
}
