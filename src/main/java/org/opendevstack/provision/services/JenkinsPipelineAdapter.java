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

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.util.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to interact with rundeck
 *
 * @author Torsten Jaeschke
 */
@Service
public class JenkinsPipelineAdapter extends BaseServiceAdapter implements IJobExecutionAdapter {

  private static final Logger logger = LoggerFactory.getLogger(JenkinsPipelineAdapter.class);

  @Value("${openshift.jenkins.webhookproxy.name.pattern}")
  private String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.jenkins.trigger.secret}")
  private String projectOpenshiftJenkinsTriggerSecret;

  @Value("${rundeck.group.pattern}")
  private String groupPattern;

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

  @Value("#{${jenkinspipeline.quickstarter}}")
  private Map<String, String> jenkinsPipelineQuickstarter;

  @Value("${jenkinspipeline.create-ods-projects-job}")
  private String jenkinsPipelineCreateOdsProjectsJob;

  @Value("${bitbucket.uri}")
  private String bitbucketUri;

  public JenkinsPipelineAdapter() {
    super("jenkinspipeline");
  }

  public List<Job> getQuickstarters() {
    return jenkinsPipelineQuickstarter.keySet().stream()
        .map(
            jobname -> {
              Job job = createJobFromUrl(jobname, jenkinsPipelineQuickstarter.get(jobname));
              logger.info("Available quickstarter: " + job);
              return job;
            })
        .collect(Collectors.toList());
  }

  private Job createJobFromUrl(String jobname, String url) {
    String gitURL = url.split("\\.git")[0];
    String gitParentProject = gitURL.split("/")[0];
    String gitRepoName = gitURL.split("/")[1];
    String jenkinsPath = url.split("\\.git")[1];
    String branch = "master";
    if (jenkinsPath.startsWith("#")) {
      Pattern pattern = Pattern.compile("#([a-zA-Z]*)\\/(.*)");
      Matcher matcher = pattern.matcher(jenkinsPath);
      matcher.find();
      branch = matcher.group(1);
      jenkinsPath = matcher.group(2);
    } else {
      jenkinsPath = jenkinsPath.substring(1);
    }
    Job job =
        new Job(
            jobname, true, jobname, jobname, gitParentProject, gitRepoName, jenkinsPath, branch);
    return job;
  }

  public Job getCreateOdsProjectsJob() {

    return createJobFromUrl(projectCreateOpenshiftJob, jenkinsPipelineCreateOdsProjectsJob);
  }

  public List<ExecutionsData> provisionComponentsBasedOnQuickstarters(OpenProjectData project)
      throws IOException {
    if (project == null || project.quickstarters == null) {
      return new ArrayList<>();
    }

    List<ExecutionsData> executionList = new ArrayList<>();
    if (project.quickstarters != null) {
      for (Map<String, String> options : project.quickstarters) {
        String jobId = options.get(OpenProjectData.COMPONENT_TYPE_KEY);
        String groupId =
            String.format(groupPattern, project.projectKey.toLowerCase()).replace('_', '-');
        String packageName =
            String.format(
                "%s.%s",
                String.format(groupPattern, project.projectKey.toLowerCase()),
                options.get(OpenProjectData.COMPONENT_ID_KEY).replace('-', '_'));

        options.put("GROUP_ID", groupId);
        options.put("PROJECT_ID", project.projectKey.toLowerCase());
        options.put("PACKAGE_NAME", packageName);

        executionList.add(prepareAndExecuteJob(jobId, options, project.webhookProxySecret));
      }
    }
    return executionList;
  }

  @Override
  public OpenProjectData createPlatformProjects(OpenProjectData project) throws IOException {

    Map<String, String> options = new HashMap<>();
    Preconditions.checkNotNull(project, "Cannot create null project");

    //    init webhook secret
    project.webhookProxySecret = UUID.randomUUID().toString();
    options.put(
        "PIPELINE_TRIGGER_SECRET",
        Base64.getEncoder().encodeToString(project.webhookProxySecret.getBytes()));

    try {
      options.put("PROJECT_ID", project.projectKey.toLowerCase());
      if (project.specialPermissionSet) {
        String entitlementGroups =
            "ADMINGROUP="
                + project.projectAdminGroup
                + ","
                + "USERGROUP="
                + project.projectUserGroup
                + ","
                + "READONLYGROUP="
                + project.projectReadonlyGroup;

        logger.debug(
            "project id: {} passed project owner: {} passed groups: {}",
            project.projectKey,
            project.projectAdminUser,
            entitlementGroups);

        options.put("PROJECT_ADMIN", project.projectAdminUser);
        options.put("PROJECT_GROUPS", entitlementGroups);
      } else {
        // someone is always logged in :)
        logger.debug("project id: {} admin: {}", project.projectKey, getUserName());
        options.put("PROJECT_ADMIN", getUserName());
      }
      ExecutionsData data =
          prepareAndExecuteJob(
              projectCreateOpenshiftJob, options, projectOpenshiftJenkinsTriggerSecret);

      // add openshift based links - for jenkins we know the link - hence create the
      // direct
      // access link to openshift app domain
      project.platformBuildEngineUrl =
          "https://"
              + String.format(
                  projectOpenshiftJenkinsProjectPattern,
                  project.projectKey.toLowerCase(),
                  projectOpenshiftBaseDomain);

      // we can only add the console based links - as no routes are created per
      // default
      project.platformDevEnvironmentUrl =
          String.format(
              projectOpenshiftDevProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.projectKey.toLowerCase());

      project.platformTestEnvironmentUrl =
          String.format(
              projectOpenshiftTestProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.projectKey.toLowerCase());

      project.lastExecutionJobs = new ArrayList<>();
      project.lastExecutionJobs.add(data.getPermalink());
      logger.debug("Project creation job: {} ", data.getPermalink());
      return project;
    } catch (IOException ex) {
      String error =
          String.format(
              "Cannot execute job for project %s, error: %s", project.projectKey, ex.getMessage());
      logger.error(error, ex);
      throw ex;
    }
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    throw new NotImplementedException();
  }

  @Override
  public String getAdapterApiUri() {
    throw new NotImplementedException();
  }

  private ExecutionsData prepareAndExecuteJob(
      String jobNameOrId, Map<String, String> options, String webhook_proxy_secret)
      throws IOException {
    Preconditions.checkNotNull(jobNameOrId, "Cannot execute Null Job!");

    String url;
    Execution execution = new Execution();

    String projID = Objects.toString(options.get("PROJECT_ID"));

    if (projectCreateOpenshiftJob.equals(jobNameOrId)) { // we create something

      Job job = getCreateOdsProjectsJob();

      String webhookProxyHost =
          String.format(
              projectOpenshiftJenkinsWebhookProxyNamePattern, "prov", projectOpenshiftBaseDomain);
      url =
          "https://"
              + webhookProxyHost
              + "/build?trigger_secret="
              + webhook_proxy_secret
              + "&jenkinsfile_path="
              + job.jenkinsfilePath
              + "&component=ods-corejob-"
              + job.name
              + "-"
              + projID;
      execution.branch = job.branch;
      execution.repository = job.gitRepoName;
      execution.project = job.gitParentProject;

    } else {
      String component_id = Objects.toString(options.get("component_id"));

      final Job job =
          getQuickstarters().stream().filter(x -> x.id.equals(jobNameOrId)).findFirst().get();

      String webhookProxyHost =
          String.format(
              projectOpenshiftJenkinsWebhookProxyNamePattern, projID, projectOpenshiftBaseDomain);
      url =
          "https://"
              + webhookProxyHost
              + "/build?trigger_secret="
              + webhook_proxy_secret
              + "&jenkinsfile_path="
              + job.jenkinsfilePath
              + "&component=ods-quickstarter-"
              + job.getName()
              + "-"
              + component_id;
      execution.branch = job.branch;
      execution.repository = job.gitRepoName;
      execution.project = job.gitParentProject;
    }
    if (options != null) {
      execution.setOptions(options);
    }
    try {
      // ExecutionsData data = restClient.callHttp(url, execution, false, RestClient.HTTP_VERB.POST,
      // ExecutionsData.class);
      String data =
          restClient.execute(
              notAuthenticatedCall(HttpVerb.POST)
                  .url(url)
                  .body(execution)
                  .returnType(String.class));
      logger.info(data);
      ExecutionsData ret = new ExecutionsData();
      ret.setMessage(data);
      return ret;
    } catch (IOException rundeckException) {
      logger.error("Error starting job {} - details:", jobNameOrId, rundeckException);
      throw rundeckException;
    }
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

    if (stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION)) {
      if (project.lastExecutionJobs != null && !project.lastExecutionJobs.isEmpty()) {
        logger.debug("Could not start delete job for project {}, {}", project.projectKey);
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
        return leftovers;

      } else {
        logger.debug("Project {} not affected from cleanup", project.projectKey);
        return leftovers;
      }
    }

    if (project.quickstarters != null || project.quickstarters.size() > 0) {

      int nonDeletedQuickstarters = 0;
      List<String> quickstartersToDelete = new ArrayList<>();
      for (Map<String, String> quickstarterOptions : project.quickstarters) {
        quickstartersToDelete.add(quickstarterOptions.get(OpenProjectData.COMPONENT_ID_KEY));
      }

      logger.debug("Cleanup of quickstarters {}", quickstartersToDelete);
      for (String quickstarterName : quickstartersToDelete) {
        logger.debug("Could not start delete job for component {}, {}", quickstarterName);
        nonDeletedQuickstarters++;
      }

      if (nonDeletedQuickstarters > 0) {
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER, nonDeletedQuickstarters);
      }
    }

    logger.debug(
        "Cleanup done - status: {} components are left ..",
        leftovers.size() == 0 ? 0 : leftovers.size());

    return leftovers;
  }
}
