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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.config.JenkinsPipelineProperties;
import org.opendevstack.provision.config.Quickstarter;
import org.opendevstack.provision.model.ExecutionJob;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jenkins.Execution;
import org.opendevstack.provision.model.jenkins.Job;
import org.opendevstack.provision.model.webhookproxy.CreateProjectResponse;
import org.opendevstack.provision.util.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to interact with Jenkins in order to provision projects and components.
 *
 * @author Torsten Jaeschke
 */
@Service
public class JenkinsPipelineAdapter extends BaseServiceAdapter implements IJobExecutionAdapter {

  private static final Logger logger = LoggerFactory.getLogger(JenkinsPipelineAdapter.class);

  @Value("${openshift.jenkins.webhookproxy.name.pattern}")
  protected String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.jenkins.trigger.secret}")
  private String projectOpenshiftJenkinsTriggerSecret;

  @Value("${artifact.group.pattern}")
  protected String groupPattern;

  @Value("${openshift.apps.basedomain}")
  protected String projectOpenshiftBaseDomain;

  @Value("${openshift.console.uri}")
  protected String projectOpenshiftConsoleUri;

  @Value("${openshift.test.project.name.pattern}")
  protected String projectOpenshiftTestProjectPattern;

  @Value("${openshift.dev.project.name.pattern}")
  protected String projectOpenshiftDevProjectPattern;

  @Value("${openshift.jenkins.project.name.pattern}")
  protected String projectOpenshiftJenkinsProjectPattern;

  @Autowired protected JenkinsPipelineProperties jenkinsPipelineProperties;

  @Value("${bitbucket.uri}")
  protected String bitbucketUri;

  @Value("${ods.image-tag}")
  private String odsImageTag;

  @Value("${ods.git-ref}")
  private String odsGitRef;

  private List<Job> componentQuickstarters;

  public JenkinsPipelineAdapter() {
    super("jenkinspipeline");
  }

  private Map<String, Job> componentTypeToJobMappings;

  @PostConstruct
  public void init() {
    componentQuickstarters =
        jenkinsPipelineProperties.getQuickstarter().values().stream()
            .filter(Quickstarter::isComponentQuickstarter)
            .map(Job::new)
            .sorted(Comparator.comparing(Job::getDescription))
            .collect(Collectors.toList());
    logger.info("All Quickstarters" + jenkinsPipelineProperties.getQuickstarter());

    Map<String, Job> nameToJobMappings =
        componentQuickstarters.stream().collect(toMap(Job::getName, job -> job));
    Map<String, Job> legacyCtToJobMappings =
        componentQuickstarters.stream()
            .filter(j -> j.legacyCt != null)
            .collect(toMap(Job::getLegacyCt, job -> job));
    nameToJobMappings.putAll(legacyCtToJobMappings);

    componentTypeToJobMappings = nameToJobMappings;
    logger.info(
        "componentTypeMappings: {}",
        componentTypeToJobMappings.entrySet().stream()
            .map(es -> es.getKey() + " -> " + es.getValue().name)
            .collect(toList()));
  }

  public JenkinsPipelineProperties getJenkinsPipelineProperties() {
    return jenkinsPipelineProperties;
  }

  public List<Job> getComponentQuickstarters() {
    return componentQuickstarters;
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
        options.put("ODS_IMAGE_TAG", odsImageTag);
        options.put("ODS_GIT_REF", odsGitRef);

        String triggerSecret =
            project.webhookProxySecret != null
                ? project.webhookProxySecret
                : projectOpenshiftJenkinsTriggerSecret;

        final Job job =
            getComponentQuickstarters().stream()
                .filter(x -> x.id.equals(jobId))
                .findFirst()
                .orElseThrow(
                    () ->
                        new RuntimeException(
                            String.format("Cannot find quickstarter with id=%s!", jobId)));
        executionList.add(prepareAndExecuteJob(job, options, triggerSecret));
      }
    }
    return executionList;
  }

  @Override
  public Optional<Job> getComponentByType(String componentType) {
    return Optional.ofNullable(componentTypeToJobMappings.get(componentType));
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

      options.put("ODS_IMAGE_TAG", odsImageTag);
      options.put("ODS_GIT_REF", odsGitRef);
      ExecutionsData data =
          prepareAndExecuteJob(
              new Job(jenkinsPipelineProperties.getCreateProjectQuickstarter()),
              options,
              projectOpenshiftJenkinsTriggerSecret);

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
      ExecutionJob executionJob = new ExecutionJob(data.getJobName(), data.getPermalink());
      project.lastExecutionJobs.add(executionJob);
      logger.debug("Project creation job: {} ", executionJob);
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
      final Job job, Map<String, String> options, String webhookProxySecret) throws IOException {

    String jobNameOrId = job.getName();
    Preconditions.checkNotNull(jobNameOrId, "Cannot execute Null Job!");
    Execution execution = buildExecutionObject(job, options, webhookProxySecret);

    try {
      CreateProjectResponse data =
          restClient.execute(
              notAuthenticatedCall(HttpVerb.POST)
                  .url(execution.url)
                  .body(execution)
                  .returnType(CreateProjectResponse.class));
      logger.info("Webhook proxy returned " + data.toString());
      ExecutionsData executionsData = new ExecutionsData();
      executionsData.setMessage(data.toString());

      String namespace = data.extractNamespace();

      String jobName = String.format("%s-%s", namespace, data.extractBuildConfigName());
      String buildNumber = data.extractBuildNumber();
      String jenkinsHost = String.format("jenkins-%s%s", namespace, projectOpenshiftBaseDomain);
      String href =
          String.format(
              "https://%s/job/%s/job/%s/%s", jenkinsHost, namespace, jobName, buildNumber);
      executionsData.setJobName(jobName);
      executionsData.setPermalink(href);
      return executionsData;
    } catch (IOException rundeckException) {
      logger.error("Error starting job {} - details:", jobNameOrId, rundeckException);
      throw rundeckException;
    }
  }

  private Execution buildExecutionObject(
      Job job, Map<String, String> options, String webhookProxySecret) {
    String projID = Objects.toString(options.get("PROJECT_ID"));
    Execution execution = new Execution();

    if (jenkinsPipelineProperties.isCreateOrDeleteProjectJob(job.getId())) {

      String webhookProxyHost =
          String.format(
              projectOpenshiftJenkinsWebhookProxyNamePattern, "prov", projectOpenshiftBaseDomain);
      execution.url =
          "https://"
              + webhookProxyHost
              + "/build?trigger_secret="
              + webhookProxySecret
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

      String webhookProxyHost =
          String.format(
              projectOpenshiftJenkinsWebhookProxyNamePattern, projID, projectOpenshiftBaseDomain);
      execution.url =
          "https://"
              + webhookProxyHost
              + "/build?trigger_secret="
              + webhookProxySecret
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
    return execution;
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(
      LIFECYCLE_STAGE stage, OpenProjectData project) {
    Preconditions.checkNotNull(stage);
    Preconditions.checkNotNull(project);

    Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers =
        stage.equals(LIFECYCLE_STAGE.INITIAL_CREATION)
            ? cleanupWholeProjects(project)
            : cleanupQuickstartersOnly(project);
    logger.debug("Cleanup done - status: {} components are left ..", leftovers.size());

    return leftovers;
  }

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupWholeProjects(OpenProjectData project) {
    if (project.lastExecutionJobs != null && !project.lastExecutionJobs.isEmpty()) {

      Map<String, String> options = new HashMap<>();
      options.put("PROJECT_ID", project.projectKey.toLowerCase());
      options.put("ODS_IMAGE_TAG", odsImageTag);
      options.put("component_id", project.projectKey.toLowerCase());
      options.put("ODS_GIT_REF", odsGitRef);

      try {
        Job job = new Job(jenkinsPipelineProperties.getDeleteProjectQuickstarter());
        logger.debug(
            "Calling delete-projects job for project {}" + " with id {}",
            project.projectKey,
            job.getId());
        ExecutionsData data =
            prepareAndExecuteJob(job, options, projectOpenshiftJenkinsTriggerSecret);
        logger.info("Result of cleanup: {}", data.toString());
        return Collections.emptyMap();
      } catch (IOException e) {
        logger.debug(
            "Could not start delete job for project {}: {}", project.projectKey, e.getMessage());
        Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT, 1);
        return leftovers;
      }
    }

    logger.debug("Project {} not affected from cleanup", project.projectKey);
    return Collections.emptyMap();
  }

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupQuickstartersOnly(
      OpenProjectData project) {

    if (CollectionUtils.isNotEmpty(project.quickstarters)) {
      List<String> quickstartersToDelete =
          project.quickstarters.stream()
              .map(q -> q.get(OpenProjectData.COMPONENT_ID_KEY))
              .collect(Collectors.toList());
      logger.debug("Cleanup of quickstarters {}", quickstartersToDelete);

      // TODO #294 delete single component one after one, remove from quickstartersToDelete list,
      // log if remove fails via
      // logger.debug("Could not start delete job for component {}", quickstarterName);

      if (!quickstartersToDelete.isEmpty()) {
        Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();
        leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER, quickstartersToDelete.size());
        return leftovers;
      } else {
        return Collections.emptyMap();
      }
    }

    logger.debug(
        "Project {} not affected from cleanup: no quickstarter defined that should be deleted.",
        project.projectKey);
    return Collections.emptyMap();
  }
}
