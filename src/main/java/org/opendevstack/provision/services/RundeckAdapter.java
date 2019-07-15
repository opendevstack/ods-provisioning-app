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
import org.opendevstack.provision.util.rest.Client;
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
public class RundeckAdapter extends BaseServiceAdapter implements IJobExecutionAdapter {

  private static final Logger logger = LoggerFactory
      .getLogger(RundeckAdapter.class);

  @Autowired
  private RundeckJobStore jobStore;

  @Value("${rundeck.api.url}")
  private String rundeckApiUrl;

  @Value("${rundeck.auth.url}")
  private String preAuthUrl;

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
  IODSAuthnzAdapter manager;

  @Autowired
  Client restClient;

  public RundeckAdapter(
      @Value("${rundeck.admin_user}") String adminUser,
      @Value("${rundeck.admin_password}") String adminPassword) {
    super(adminUser, adminPassword);
  }


  public List<Job> getQuickstarters() {
    try {
      return getJobs(projectQuickstarterGroup);
    } catch (IOException ex) {
      logger.error(GENERIC_RUNDECK_ERRMSG, ex);
    }
    return new ArrayList<>();
  }

  public List<ExecutionsData> provisionComponentsBasedOnQuickstarters(
      OpenProjectData project) throws IOException {

        List<ExecutionsData> executionList = new ArrayList<>();

        if (jobStore.size() == 0)
        {
            jobStore.addJobs(getJobs(projectOpenshiftGroup));
        }

        if (project.quickstarters != null)
        {
            for (Map<String, String> options : project.quickstarters)
            {
                String jobId = options.get(
                        OpenProjectData.COMPONENT_TYPE_KEY);
                String quickstarterName = options.get(
                        OpenProjectData.COMPONENT_ID_KEY);

                Job job = jobStore
                        .getJob(jobId);

                if (job == null) {
                    throw new IOException(
                            String.format("Cannot find job with id: %s, jobs: %s"
                                    + " to provision quickstarter {}",
                                    jobId, jobStore.toString(), quickstarterName));
                }

                String url = String.format("%s/job/%s/run",
                        rundeckApiUrl, job.getId());
                String groupId = String
                        .format(groupPattern,
                                project.projectKey.toLowerCase())
                        .replace('_', '-');
                String packageName = String.format("%s.%s",
                        String.format(groupPattern,
                                project.projectKey.toLowerCase()),
                        options.get(OpenProjectData.COMPONENT_ID_KEY).
                                replace('-', '_'));
                Execution execution = new Execution();

        options.put("group_id", groupId);
        options.put("project_id", project.projectKey.toLowerCase());
        options.put("package_name", packageName);
        execution.setOptions(options);
        try {
          ExecutionsData data = callExecution(url, execution);

                    if (data.getError())
                    {
                        throw new IOException(
                                data.getMessage());
                    }

                    executionList.add(data);
                    if (data.getPermalink() != null)
                    {
                        options.put("joblink", data.getPermalink());
                    }
                } catch (Exception rundeckException)
                {
                    logger.error("Error starting job for quickstarter {} - details:",
                            quickstarterName, rundeckException);
                    throw rundeckException;
                }
            }
        }
        return executionList;
    }

  public OpenProjectData createPlatformProjects(OpenProjectData project)
      throws IOException {

    if (project == null) {
      throw new IOException("Cannot create null project");
    }

    try {
      List<Job> jobs = getJobs(projectOpenshiftGroup);
      for (Job job : jobs) {
        if (job.getName()
            .equalsIgnoreCase(projectCreateOpenshiftJob)) {
          String url = String.format("%s/job/%s/run",
              rundeckApiUrl, job.getId());
          Execution execution = new Execution();
          Map<String, String> options = new HashMap<>();
          options.put("project_id",
              project.projectKey.toLowerCase());
          if (project.specialPermissionSet) {
            String entitlementGroups = "ADMINGROUP="
                + project.projectAdminGroup + ","
                + "USERGROUP=" + project.projectUserGroup
                + "," + "READONLYGROUP="
                + project.projectReadonlyGroup;

            logger.debug(
                "project id: {} passed project owner: {} passed groups: {}",
                project.projectKey, project.projectAdminUser,
                entitlementGroups);

            options.put("project_admin", project.projectAdminUser);
            options.put("project_groups",
                entitlementGroups);
          } else {
            // someone is always logged in :)
            logger.debug("project id: {} admin: {}",
                project.projectKey, manager.getUserName());
            options.put("project_admin",
                manager.getUserName());
          }
          execution.setOptions(options);

          ExecutionsData data = callExecution(url, execution);

          // add openshift based links - for jenkins we know the link - hence create the
          // direct
          // access link to openshift app domain
          project.platformBuildEngineUrl = "https://" + String
              .format(projectOpenshiftJenkinsProjectPattern,
                  project.projectKey.toLowerCase(),
                  projectOpenshiftBaseDomain);

          // we can only add the console based links - as no routes are created per
          // default
          project.platformDevEnvironmentUrl = String.format(
              projectOpenshiftDevProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.projectKey.toLowerCase());

          project.platformTestEnvironmentUrl = String
              .format(projectOpenshiftTestProjectPattern,
                  projectOpenshiftConsoleUri.trim(),
                  project.projectKey.toLowerCase());

          project.lastExecutionJobs = new ArrayList<>();
          project.lastExecutionJobs.add(data.getPermalink());

          return project;
        }
      }
    } catch (IOException ex) {
      logger.error(GENERIC_RUNDECK_ERRMSG, ex);
      throw ex;
    }
    return project;
  }

  protected List<Job> getJobs(String group) throws IOException {
    List<Job> enabledJobs;

    String jobsUrl = String.format("%s/project/%s/jobs",
        rundeckApiUrl, rundeckProject);

    Map<String, String> jobPath =
        new HashMap<>();
    jobPath.put("groupPath", group);

    /*
    List<Job> jobs = callHttpWithTypeRefWithoutAuthentication(
        jobsUrl, jobPath, RestClient.HTTP_VERB.GET,
        new TypeReference<List<Job>>() {
        });
     */

    List<Job> jobs =restClient
        .get()
        .url(jobsUrl)
        .queryParams(jobPath)
        .preAuthenticated()
        .preAuthUrl(preAuthUrl)
        .preAuthContent(getPreAuthContent())
        .returnTypeReference(new TypeReference<List<Job>>() {})
        .execute();

    enabledJobs = jobs.stream().filter(Job::isEnabled)
        .collect(Collectors.toList());
    jobStore.addJobs(enabledJobs);
    return enabledJobs;
  }

  private Map<String, String> getPreAuthContent(){
    Map<String, String> content = new HashMap<>();
    content.put("j_username", userName);
    content.put("j_password", userPassword );
    return content;
  }

  private ExecutionsData callExecution(String url, Execution execution) throws IOException {
    return restClient
            .post()
            .url(url)
            .body(execution)
            .preAuthenticated()
            .preAuthUrl(preAuthUrl)
            .preAuthContent(getPreAuthContent())
            .returnType(ExecutionsData.class)
            .execute();
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException();
  }

  @Override
  public String getAdapterApiUri() {
    return rundeckApiUrl;
  }

    @Override
    public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup
        (LIFECYCLE_STAGE stage, OpenProjectData project)
    {
        Preconditions.checkNotNull(stage);
        Preconditions.checkNotNull(project);

        Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers =
                new HashMap<>();

        if (stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION))
        {
            if (project.lastExecutionJobs != null &&
                    !project.lastExecutionJobs.isEmpty())
            {
                String deleteProjectJob =
                        jobStore.getJobIdForJobName("delete-projects");
                String deleteProjectJobUrl = String.format("%s%s/job/%s/run",
                        rundeckApiUrl, deleteProjectJob);
                if (deleteProjectJob == null)
                {
                    logger.error("Cannot find delete-projects job, hence"
                            + " cannot delete project!");
                    leftovers.put(
                            CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
                } else
                {
                    logger.debug("Calling delete-projects job for project {}" +
                            " with id {}",
                            project.projectKey, deleteProjectJob);

                    Execution execution = new Execution();
                    Map<String, String> options = new HashMap<>();
                        options.put("project_id",
                            project.projectKey.toLowerCase());

                    try
                    {
                        ExecutionsData cleanupData = client.callHttp(
                                deleteProjectJobUrl,
                                execution, false,
                                RestClient.HTTP_VERB.POST,
                                ExecutionsData.class);
                    } catch (Exception allExecExceptions)
                    {
                        logger.debug("Could not start delete job for project {}, {}",
                                project.projectKey, allExecExceptions.getMessage());
                        leftovers.put(
                                CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
                        return leftovers;
                    }
                    return leftovers;
                }
                return leftovers;
            } else
            {
            logger.debug("Project {} not affected from cleanup",
                    project.projectKey);
            return leftovers;

            }
        }

        if  (project.quickstarters != null ||
                project.quickstarters.size() > 0)
        {
            String deleteComponentJob =
                    jobStore.getJobIdForJobName("delete-component");
            if (deleteComponentJob == null)
            {
                logger.error("Cannot find delete-components job, hence"
                        + " cannot clean quickstarters!");

                leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER,
                        project.quickstarters.size());
                return leftovers;
            }

            String deleteComponentJobUrl = String.format("%s%s/job/%s/run",
                    rundeckApiUrl, deleteComponentJob);

            int nonDeletedQuickstarters = 0;
            List<String> quickstartersToDelete =
                    new ArrayList<>();
            for (Map<String, String> quickstarterOptions :
                project.quickstarters)
            {
                quickstartersToDelete.add(quickstarterOptions.get(
                        OpenProjectData.COMPONENT_ID_KEY));
            }

            logger.debug("Cleanup of quickstarters {}",
                    quickstartersToDelete);
            for (String quickstarterName : quickstartersToDelete)
            {
                logger.debug("Cleanup of quickstarter {} thru job {}",
                        quickstarterName, deleteComponentJob);

                Execution execution = new Execution();
                Map<String, String> options = new HashMap<>();
                    options.put("project_id", project.projectKey.toLowerCase());
                    options.put("component_id", quickstarterName);
                try
                {
                    ExecutionsData cleanupData = client.callHttp(
                            deleteComponentJobUrl,
                            execution, false,
                            RestClient.HTTP_VERB.POST,
                            ExecutionsData.class);
                } catch (Exception allExecExceptions)
                {
                    logger.debug("Could not start delete job for component {}, {}",
                            quickstarterName, allExecExceptions.getMessage());
                    nonDeletedQuickstarters++;
                }
            }

            if (nonDeletedQuickstarters > 0)
            {
                leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER,
                        nonDeletedQuickstarters);
            }
        }

        logger.debug("Cleanup done - status: {} components are left ..",
                leftovers.size() == 0 ? 0 : leftovers);

        return leftovers;
    }
}
