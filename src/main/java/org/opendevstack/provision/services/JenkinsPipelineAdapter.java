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

import static java.util.stream.Collectors.toMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.NotImplementedException;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.config.JenkinsPipelineProperties;
import org.opendevstack.provision.config.Quickstarter;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.ExecutionJob;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.OpenProjectDataValidator;
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
 */
@Service
public class JenkinsPipelineAdapter extends BaseServiceAdapter implements IJobExecutionAdapter {

  private static final Logger logger = LoggerFactory.getLogger(JenkinsPipelineAdapter.class);

  public static final String EXECUTION_URL_COMP_PREFIX = "ods-qs";
  public static final String EXECUTION_URL_ADMIN_JOB_COMP_PREFIX = "ods-corejob";

  public static final List<Consumer<Map<String, String>>> COMPONENT_ID_VALIDATOR_LIST =
      Arrays.asList(
          OpenProjectDataValidator.createComponentIdValidator(
              OpenProjectDataValidator.COMPONENT_ID_MIN_LENGTH,
              OpenProjectDataValidator.COMPONENT_ID_MAX_LENGTH));

  public static final List<Consumer<Map<String, String>>> PROJECT_ID_VALIDATOR_LIST =
      Arrays.asList(
          OpenProjectDataValidator.createProjectIdValidator(
              OpenProjectDataValidator.COMPONENT_ID_MIN_LENGTH,
              OpenProjectDataValidator.COMPONENT_ID_MAX_LENGTH));

  public static final String PROJECT_ID_KEY = "PROJECT_ID";

  public static final String OPTION_KEY_GIT_SERVER_URL = "GIT_SERVER_URL";
  public static final String CREATE_PROJECTS_JOB_ID = JenkinsPipelineProperties.CREATE_PROJECTS;
  public static final String DELETE_PROJECTS_JOB_ID = JenkinsPipelineProperties.DELETE_PROJECTS;

  @Value("${openshift.jenkins.admin.webhookproxy.host}")
  protected String adminWebhookProxyHost;

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
  protected String projectWebhookProxyHostPattern;

  @Value("${openshift.jenkins.trigger.secret}")
  private String openshiftJenkinsTriggerSecret;

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

  @Value("${openshift.cd.project.name.pattern}")
  protected String projectOpenshiftCdProjectPattern;

  @Value("${openshift.jenkins.project.name.pattern}")
  protected String projectOpenshiftJenkinsProjectPattern;

  @Autowired protected JenkinsPipelineProperties jenkinsPipelineProperties;

  @Value("${bitbucket.uri}")
  protected String bitbucketUri;

  @Value("${bitbucket.opendevstack.project}")
  protected String bitbucketOdsProject;

  @Value("${ods.namespace}")
  protected String odsNamespace;

  @Value("${ods.image-tag}")
  protected String odsImageTag;

  @Value("${ods.git-ref}")
  protected String odsGitRef;

  private List<Job> quickstarterJobs;

  @Value("${bitbucket.technical.user}")
  protected String generalCdUser;

  public JenkinsPipelineAdapter() {
    super("jenkinspipeline");
  }

  private Map<String, Job> nameToJobMappings;

  private Map<String, String> legacyComponentTypeToNameMappings;

  @PostConstruct
  public void init() {
    quickstarterJobs = convertQuickstarterToJobs(jenkinsPipelineProperties.getQuickstarter());
    logger.info("All Quickstarters:" + jenkinsPipelineProperties.getQuickstarter());
    logger.info("All Adminjobs:" + jenkinsPipelineProperties.getAdminjobs());

    nameToJobMappings = quickstarterJobs.stream().collect(toMap(Job::getName, job -> job));
    legacyComponentTypeToNameMappings =
        ImmutableMap.<String, String>builder()
            .put("e5b77f0f-262a-42f9-9d06-5d9052c1f394", "be-java-springboot")
            .put("e59e71f5-76e0-4b8c-b040-a526197ee84d", "docker-plain")
            .put("f3a7717d-f51a-426c-82fe-4574d4e595ad", "be-golang-plain")
            .put("9992a587-959c-4ceb-8e3f-c1390e40c582", "be-python-flask")
            .put("14ce143c-7d2a-11e7-bb31-be2e44b06b34", "be-scala-akka")
            .put("7f98bafb-c81d-4eb0-aad1-700b6c05fc12", "be-typescript-express")
            .put("a7b930b2-d125-48ce-9997-9643faa9cdd0", "ds-jupyter-notebook")
            .put("69405fd4-b0c2-45a8-a6dc-0870ea56166e", "ds-rshiny")
            .put("1deb3f34-5cd4-439b-b987-440dc6591fdf", "ds-ml-service")
            .put("560954ef-d245-456c-9460-6c592c9d7784", "fe-angular")
            .put("a86d6f06-cedc-4c16-a92c-5ca48e400c3a", "fe-react")
            .put("15b927c0-f46b-46a6-984b-2bf5c4c2c756", "fe-vue")
            .put("6b205842-6321-4ade-b094-219b78d5acc0", "fe-ionic")
            .put("7d10fbfe-e129-4bab-87f5-4cc2de89f071", "airflow-cluster")
            .put("48c077f7-8bda-4f05-af5a-6fe085c9d405", "release-manager")
            .build();

    logger.info("legacyComponentTypeToNameMappings: {}", legacyComponentTypeToNameMappings);
  }

  private List<Job> convertQuickstarterToJobs(Map<String, Quickstarter> quickstarterMap) {
    return quickstarterMap.values().stream()
        .map(qs -> new Job(qs, odsGitRef))
        .sorted(Comparator.comparing(Job::getDescription))
        .collect(Collectors.toList());
  }

  public List<Job> getQuickstarterJobs() {
    return quickstarterJobs;
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
        options.put(PROJECT_ID_KEY, project.projectKey.toLowerCase());
        options.put("PACKAGE_NAME", packageName);
        options.put("ODS_NAMESPACE", odsNamespace);
        options.put("ODS_BITBUCKET_PROJECT", bitbucketOdsProject);
        options.put("ODS_IMAGE_TAG", odsImageTag);
        options.put("ODS_GIT_REF", odsGitRef);

        String triggerSecret =
            project.webhookProxySecret != null
                ? project.webhookProxySecret
                : openshiftJenkinsTriggerSecret;

        final Job job =
            getQuickstarterJobs().stream()
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

  private Optional<Job> getComponentByName(String name) {
    return Optional.ofNullable(nameToJobMappings.get(name));
  }

  @Override
  public Optional<Job> getComponentByType(String componentType) {
    Optional<String> maybeName =
        Optional.ofNullable(legacyComponentTypeToNameMappings.get(componentType));

    if (maybeName.isPresent()) {
      return maybeName.flatMap(this::getComponentByName);
    } else {
      return getComponentByName(componentType);
    }
  }

  @Override
  public OpenProjectData createPlatformProjects(OpenProjectData project) throws IOException {

    Map<String, String> options = new HashMap<>();
    Preconditions.checkNotNull(project, "Cannot create null project");
    validateComponentNames(
        OpenProjectDataValidator.API_COMPONENT_ID_VALIDATOR_LIST, project.getQuickstarters());

    options.put(
        "PIPELINE_TRIGGER_SECRET",
        Base64.getEncoder().encodeToString(project.webhookProxySecret.getBytes()));

    String projectCdUser = generalCdUser;
    String cdUserType = "general";
    if (project.cdUser != null && !project.cdUser.trim().isEmpty()) {
      projectCdUser = project.cdUser;
      cdUserType = "project";
    }

    options.put("CD_USER_TYPE", cdUserType);
    options.put("CD_USER_ID_B64", Base64.getEncoder().encodeToString(projectCdUser.getBytes()));

    try {
      options.put(PROJECT_ID_KEY, project.projectKey.toLowerCase());
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

      options.put("ODS_NAMESPACE", odsNamespace);
      options.put("ODS_BITBUCKET_PROJECT", bitbucketOdsProject);
      options.put("ODS_IMAGE_TAG", odsImageTag);
      options.put("ODS_GIT_REF", odsGitRef);
      options.put(OPTION_KEY_GIT_SERVER_URL, bitbucketUri);

      ExecutionsData data =
          prepareAndExecuteJob(
              new Job(jenkinsPipelineProperties.getCreateProjectQuickstarter(), odsGitRef),
              options,
              openshiftJenkinsTriggerSecret);

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
      project.platformCdEnvironmentUrl =
          String.format(
              projectOpenshiftCdProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.projectKey.toLowerCase());

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
    throw new NotImplementedException("JenkinsPipelineAdapter#getProjects");
  }

  @Override
  public String getAdapterApiUri() {
    throw new NotImplementedException("JenkinsPipelineAdapter#getAdapterApiUri");
  }

  private ExecutionsData prepareAndExecuteJob(
      final Job job, Map<String, String> options, String webhookProxySecret) throws IOException {

    String jobNameOrId = job.getName();
    Preconditions.checkNotNull(jobNameOrId, "Cannot execute Null Job!");
    Execution execution = buildExecutionObject(job, options, webhookProxySecret);

    try {
      CreateProjectResponse data =
          getRestClient()
              .execute(
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
    } catch (IOException ex) {
      String url = null != execution.url ? execution.url : "null'";
      logger.error("Error starting job {} for url '{}' - details:", jobNameOrId, url, ex);
      throw ex;
    }
  }

  private Execution buildExecutionObject(
      Job job, Map<String, String> options, String webhookProxySecret) {

    String projID = Objects.toString(options.get(PROJECT_ID_KEY));
    Execution execution = new Execution();

    String componentId = Objects.toString(options.get(OpenProjectData.COMPONENT_ID_KEY));

    if (jenkinsPipelineProperties.isAdminjob(job.getId())) {

      boolean deleteComponentJob = jenkinsPipelineProperties.isDeleteComponentJob(job.getId());
      String webhookProxyHost = computeWebhookProxyHost(job.getId(), projID);
      String url =
          buildAdminJobExecutionUrl(
              job, componentId, projID, webhookProxySecret, webhookProxyHost, deleteComponentJob);
      execution.url = url;
      execution.branch = job.branch;
      execution.repository = job.gitRepoName;
      execution.project = bitbucketOdsProject;

    } else {
      String webhookProxyHost = computeWebhookProxyHost(job.getId(), projID);

      execution.url =
          JenkinsPipelineAdapter.buildExecutionUrlQuickstarterJob(
              job, componentId, webhookProxySecret, webhookProxyHost);
      execution.branch = job.branch;
      execution.repository = job.gitRepoName;
      execution.project = bitbucketOdsProject;
    }
    if (options != null) {
      execution.setOptions(options);
    }

    logger.info("Execution url={}", execution.url);

    return execution;
  }

  private String computeWebhookProxyHost(String jobId, String projID) {
    if (jenkinsPipelineProperties.isDeleteComponentJob(jobId)) {
      return String.format(projectWebhookProxyHostPattern, projID, projectOpenshiftBaseDomain);
    } else if (jenkinsPipelineProperties.isAdminjob(jobId)) {
      return adminWebhookProxyHost + projectOpenshiftBaseDomain;
    } else {
      return String.format(projectWebhookProxyHostPattern, projID, projectOpenshiftBaseDomain);
    }
  }

  private static String buildExecutionBaseUrl(
      Job job, String webhookProxySecret, String webhookProxyHost) {
    return "https://"
        + webhookProxyHost
        + "/build?trigger_secret="
        + webhookProxySecret
        + "&jenkinsfile_path="
        + job.jenkinsfilePath;
  }

  public static String buildAdminJobExecutionUrl(
      Job job,
      String componentId,
      String projID,
      String webhookProxySecret,
      String webhookProxyHost,
      boolean deleteComponentJob) {
    String baseUrl = buildExecutionBaseUrl(job, webhookProxySecret, webhookProxyHost);
    String componentName =
        EXECUTION_URL_ADMIN_JOB_COMP_PREFIX + "-" + (deleteComponentJob ? componentId : projID);
    // yes, validating component name before calling jenkins
    if (job.id.equals(CREATE_PROJECTS_JOB_ID)) {
      validateComponentName(PROJECT_ID_VALIDATOR_LIST, componentName);
    }
    return baseUrl + "&component=" + componentName;
  }

  public static String buildExecutionUrlQuickstarterJob(
      Job job, String componentId, String webhookProxySecret, String webhookProxyHost) {
    String baseUrl = buildExecutionBaseUrl(job, webhookProxySecret, webhookProxyHost);
    String componentName = EXECUTION_URL_COMP_PREFIX + "-" + componentId;
    // yes, validating component name before calling jenkins
    validateComponentName(COMPONENT_ID_VALIDATOR_LIST, componentName);
    return baseUrl + "&component=" + componentName;
  }

  private static void validateComponentNames(
      List<Consumer<Map<String, String>>> validators, List<Map<String, String>> componentNames) {

    validators.forEach(
        validator -> {
          componentNames.stream().forEach(validator);
        });
  }

  // In this context/method component names could be a project name or quickstarter name
  private static void validateComponentName(
      List<Consumer<Map<String, String>>> validators, String componentName) {

    HashMap<String, String> components = new HashMap();
    components.put(OpenProjectData.COMPONENT_ID_KEY, componentName);

    validateComponentNames(validators, Arrays.asList(components));
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
      String componentId = project.projectKey.toLowerCase();
      Quickstarter deleteProjectAdminJob =
          jenkinsPipelineProperties.getDeleteProjectsQuickstarter();

      CLEANUP_LEFTOVER_COMPONENTS objectType = CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT;
      // Note: the secret passed here is the corresponding to the ODS CD webhook proxy
      return runDeleteAdminJob(
          deleteProjectAdminJob,
          project.projectKey,
          openshiftJenkinsTriggerSecret,
          componentId,
          objectType);
    }

    logger.debug("Project {} not affected from cleanup", project.projectKey);
    return Collections.emptyMap();
  }

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupQuickstartersOnly(
      OpenProjectData project) {

    int leftoverCount =
        project.getQuickstarters().stream()
            .map(q1 -> q1.get(OpenProjectData.COMPONENT_ID_KEY))
            .map(
                component ->
                    runDeleteAdminJob(
                        jenkinsPipelineProperties.getDeleteComponentsQuickstarter(),
                        project.projectKey,
                        project.webhookProxySecret, // Note: the secret passed here is the
                        // corresponding to the project CD webhook proxy
                        component,
                        CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER))
            .filter(m -> !m.isEmpty())
            .mapToInt(e -> 1)
            .sum();

    if (leftoverCount > 0) {
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();
      leftovers.put(CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER, leftoverCount);
      return leftovers;
    } else {
      return Collections.emptyMap();
    }
  }

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> runDeleteAdminJob(
      Quickstarter adminQuickstarter,
      String projectKey,
      String webhookProxySecret,
      String componentId,
      CLEANUP_LEFTOVER_COMPONENTS objectType) {
    String projectId = projectKey.toLowerCase();
    Map<String, String> options = buildAdminJobOptions(projectId, componentId);
    Job job = new Job(adminQuickstarter, odsGitRef);
    try {

      logger.debug("Calling job {} for project {}", job.getId(), projectKey);
      ExecutionsData data = prepareAndExecuteJob(job, options, webhookProxySecret);
      logger.info("Result of cleanup: {}", data.toString());
      return Collections.emptyMap();
    } catch (RuntimeException | IOException e) {
      logger.debug(
          "Could not start job {} for project {}/component {} : {}",
          job.getId(),
          projectKey,
          componentId,
          e.getMessage());
      Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> leftovers = new HashMap<>();

      leftovers.put(objectType, 1);
      return leftovers;
    }
  }

  private Map<String, String> buildAdminJobOptions(String projectId, String componentId) {
    Map<String, String> options = new HashMap<>();

    options.put(PROJECT_ID_KEY, projectId);
    options.put(OpenProjectData.COMPONENT_ID_KEY, componentId);
    options.put("ODS_NAMESPACE", odsNamespace);
    options.put("ODS_BITBUCKET_PROJECT", bitbucketOdsProject);
    options.put("ODS_IMAGE_TAG", odsImageTag);
    options.put("ODS_GIT_REF", odsGitRef);
    return options;
  }

  @Override
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {
    throw new UnsupportedOperationException("not implemented yet!");
  }
}
