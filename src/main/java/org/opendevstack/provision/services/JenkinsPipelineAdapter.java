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
import org.springframework.util.Assert;

/** Service to interact with Jenkins in order to provision projects and components. */
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
  private String adminWebhookProxyHost;

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
  private String projectWebhookProxyHostPattern;

  @Value("${openshift.jenkins.trigger.secret}")
  private String openshiftJenkinsTriggerSecret;

  @Value("${artifact.group.pattern}")
  private String groupPattern;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Value("${openshift.console.uri}")
  private String projectOpenshiftConsoleUri;

  @Value("${openshift.test.project.name.pattern}")
  private String projectOpenshiftTestProjectPattern;

  @Value("${openshift.dev.project.name.pattern}")
  private String projectOpenshiftDevProjectPattern;

  @Value("${openshift.cd.project.name.pattern}")
  private String projectOpenshiftCdProjectPattern;

  @Value("${openshift.jenkins.project.name.pattern}")
  private String projectOpenshiftJenkinsProjectPattern;

  @Autowired private JenkinsPipelineProperties jenkinsPipelineProperties;

  @Value("${bitbucket.uri}")
  private String bitbucketUri;

  @Value("${bitbucket.opendevstack.project}")
  private String bitbucketOdsProject;

  @Value("${ods.namespace}")
  private String odsNamespace;

  @Value("${ods.image-tag}")
  private String odsImageTag;

  @Value("${ods.git-ref}")
  private String odsGitRef;

  private List<Job> quickstarterJobs;

  @Value("${bitbucket.technical.user}")
  private String generalCdUser;

  @Value("${jenkinspipeline.create-project.default-project-groups:''}")
  private String defaultEntitlementGroups;

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
    if (project == null || project.getQuickstarters() == null) {
      return new ArrayList<>();
    }

    List<ExecutionsData> executionList = new ArrayList<>();
    if (project.getQuickstarters() != null) {
      for (Map<String, String> options : project.getQuickstarters()) {
        String jobId = options.get(OpenProjectData.COMPONENT_TYPE_KEY);
        String groupId =
            String.format(groupPattern, project.getProjectKey().toLowerCase()).replace('_', '-');
        String packageName =
            String.format(
                "%s.%s",
                String.format(groupPattern, project.getProjectKey().toLowerCase()),
                options.get(OpenProjectData.COMPONENT_ID_KEY).replace('-', '_'));

        options.put("GROUP_ID", groupId);
        options.put(PROJECT_ID_KEY, project.getProjectKey().toLowerCase());
        options.put("PACKAGE_NAME", packageName);
        options.put("ODS_NAMESPACE", odsNamespace);
        options.put("ODS_BITBUCKET_PROJECT", bitbucketOdsProject);
        options.put("ODS_IMAGE_TAG", odsImageTag);
        options.put("ODS_GIT_REF", odsGitRef);

        String triggerSecret =
            project.getWebhookProxySecret() != null
                ? project.getWebhookProxySecret()
                : openshiftJenkinsTriggerSecret;

        final Job job =
            getQuickstarterJobs().stream()
                .filter(x -> x.getId().equals(jobId))
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
        Base64.getEncoder().encodeToString(project.getWebhookProxySecret().getBytes()));

    String projectCdUser = generalCdUser;
    String cdUserType = "general";
    if (project.getCdUser() != null && !project.getCdUser().trim().isEmpty()) {
      projectCdUser = project.getCdUser();
      cdUserType = "project";
    }

    options.put("CD_USER_TYPE", cdUserType);
    options.put("CD_USER_ID_B64", Base64.getEncoder().encodeToString(projectCdUser.getBytes()));

    try {
      options.put(PROJECT_ID_KEY, project.getProjectKey().toLowerCase());

      if (addDefaultEntitlementGroups()) {
        addOrAppendToEntry(options, "PROJECT_GROUPS", defaultEntitlementGroups, ",");
      }

      if (project.isSpecialPermissionSet()) {
        String entitlementGroups =
            "ADMINGROUP="
                + project.getProjectAdminGroup()
                + ","
                + "USERGROUP="
                + project.getProjectUserGroup()
                + ","
                + "READONLYGROUP="
                + project.getProjectReadonlyGroup();

        logger.debug(
            "project id: {} passed project owner: {} passed groups: {}",
            project.getProjectKey(),
            project.getProjectAdminUser(),
            entitlementGroups);

        options.put("PROJECT_ADMIN", project.getProjectAdminUser());
        addOrAppendToEntry(options, "PROJECT_GROUPS", entitlementGroups, ",");

      } else {
        // someone is always logged in :)
        logger.debug("project id: {} admin: {}", project.getProjectKey(), getUserName());
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
      project.setPlatformBuildEngineUrl(
          "https://"
              + String.format(
                  projectOpenshiftJenkinsProjectPattern,
                  project.getProjectKey().toLowerCase(),
                  projectOpenshiftBaseDomain));

      // we can only add the console based links - as no routes are created per
      // default
      project.setPlatformCdEnvironmentUrl(
          String.format(
              projectOpenshiftCdProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.getProjectKey().toLowerCase()));

      project.setPlatformDevEnvironmentUrl(
          String.format(
              projectOpenshiftDevProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.getProjectKey().toLowerCase()));

      project.setPlatformTestEnvironmentUrl(
          String.format(
              projectOpenshiftTestProjectPattern,
              projectOpenshiftConsoleUri.trim(),
              project.getProjectKey().toLowerCase()));

      project.setLastExecutionJobs(new ArrayList<>());
      ExecutionJob executionJob = new ExecutionJob(data.getJobName(), data.getPermalink());
      project.getLastExecutionJobs().add(executionJob);
      logger.debug("Project creation job: {} ", executionJob);
      return project;
    } catch (IOException ex) {
      String error =
          String.format(
              "Cannot execute job for project %s, error: %s",
              project.getProjectKey(), ex.getMessage());
      logger.error(error, ex);
      throw ex;
    }
  }

  public static void addOrAppendToEntry(
      Map<String, String> map, String key, String toAppend, String separator) {
    Assert.notNull(map, "Parameter map is null!");
    Assert.notNull(toAppend, "Parameter toAppend is null!");
    Assert.notNull(key, "Parameter key is null!");
    Assert.notNull(separator, "Parameter separator is null!");

    String value = map.computeIfPresent(key, (s, s2) -> s2 + separator + toAppend);
    if (value != null) {
      return;
    }

    map.put(key, map.computeIfAbsent(key, s -> toAppend));
  }

  private boolean addDefaultEntitlementGroups() {
    return null != defaultEntitlementGroups && !defaultEntitlementGroups.isEmpty();
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
                      .url(execution.getUrl())
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
      String url = null != execution.getUrl() ? execution.getUrl() : "null'";
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
      execution.setUrl(url);
      execution.setBranch(job.getBranch());
      execution.setRepository(job.getGitRepoName());
      execution.setProject(bitbucketOdsProject);

    } else {
      String webhookProxyHost = computeWebhookProxyHost(job.getId(), projID);

      execution.setUrl(
          JenkinsPipelineAdapter.buildExecutionUrlQuickstarterJob(
              job, componentId, webhookProxySecret, webhookProxyHost));
      execution.setBranch(job.getBranch());
      execution.setRepository(job.getGitRepoName());
      execution.setProject(bitbucketOdsProject);
    }
    if (options != null) {
      execution.setOptions(options);
    }

    logger.info("Execution url={}", execution.getUrl());

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
        + job.getJenkinsfilePath();
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
    if (job.getId().equals(CREATE_PROJECTS_JOB_ID)) {
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
    if (project.getLastExecutionJobs() != null && !project.getLastExecutionJobs().isEmpty()) {
      String componentId = project.getProjectKey().toLowerCase();
      Quickstarter deleteProjectAdminJob =
          jenkinsPipelineProperties.getDeleteProjectsQuickstarter();

      CLEANUP_LEFTOVER_COMPONENTS objectType = CLEANUP_LEFTOVER_COMPONENTS.PLTF_PROJECT;
      // Note: the secret passed here is the corresponding to the ODS CD webhook proxy
      return runDeleteAdminJobAndSetAsLastExecutionJobToProject(
          deleteProjectAdminJob,
          project.getProjectKey(),
          openshiftJenkinsTriggerSecret,
          componentId,
          objectType,
          project);
    }

    logger.debug("Project {} not affected from cleanup", project.getProjectKey());
    return Collections.emptyMap();
  }

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanupQuickstartersOnly(
      OpenProjectData project) {

    int leftoverCount =
        project.getQuickstarters().stream()
            .map(q1 -> q1.get(OpenProjectData.COMPONENT_ID_KEY))
            .map(
                component ->
                    runDeleteAdminJobAndSetAsLastExecutionJobToProject(
                        jenkinsPipelineProperties.getDeleteComponentsQuickstarter(),
                        project.getProjectKey(),
                        project.getWebhookProxySecret(), // Note: the secret passed here is the
                        // corresponding to the project CD webhook proxy
                        component,
                        CLEANUP_LEFTOVER_COMPONENTS.QUICKSTARTER,
                        project))
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

  private Map<CLEANUP_LEFTOVER_COMPONENTS, Integer>
      runDeleteAdminJobAndSetAsLastExecutionJobToProject(
          Quickstarter adminQuickstarter,
          String projectKey,
          String webhookProxySecret,
          String componentId,
          CLEANUP_LEFTOVER_COMPONENTS objectType,
          OpenProjectData project) {
    String projectId = projectKey.toLowerCase();
    Map<String, String> options = buildAdminJobOptions(projectId, componentId);
    Job job = new Job(adminQuickstarter, odsGitRef);
    try {

      logger.debug("Calling job {} for project {}", job.getId(), projectKey);
      ExecutionsData data = prepareAndExecuteJob(job, options, webhookProxySecret);
      logger.info("Result of cleanup: {}", data.toString());

      if (project.getLastExecutionJobs() == null) {
        project.setLastExecutionJobs(new ArrayList<ExecutionJob>());
      }

      ExecutionJob result = new ExecutionJob();
      result.setName(data.getJobName());
      result.setUrl(data.getPermalink());

      project.getLastExecutionJobs().add(result);

      return Collections.emptyMap();
    } catch (RuntimeException | IOException e) {
      logger.debug(
          "Could not start job {} for project {}/component {} : {}",
          job.getId(),
          projectKey,
          componentId,
          e.getMessage(),
          e);
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
  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(
      OpenProjectData newProject) {
    throw new UnsupportedOperationException("not implemented yet!");
  }

  public String getAdminWebhookProxyHost() {
    return adminWebhookProxyHost;
  }

  public void setAdminWebhookProxyHost(String adminWebhookProxyHost) {
    this.adminWebhookProxyHost = adminWebhookProxyHost;
  }

  public void setProjectWebhookProxyHostPattern(String projectWebhookProxyHostPattern) {
    this.projectWebhookProxyHostPattern = projectWebhookProxyHostPattern;
  }

  public void setGroupPattern(String groupPattern) {
    this.groupPattern = groupPattern;
  }

  public void setProjectOpenshiftBaseDomain(String projectOpenshiftBaseDomain) {
    this.projectOpenshiftBaseDomain = projectOpenshiftBaseDomain;
  }

  public void setProjectOpenshiftConsoleUri(String projectOpenshiftConsoleUri) {
    this.projectOpenshiftConsoleUri = projectOpenshiftConsoleUri;
  }

  public String getProjectOpenshiftTestProjectPattern() {
    return projectOpenshiftTestProjectPattern;
  }

  public void setProjectOpenshiftTestProjectPattern(String projectOpenshiftTestProjectPattern) {
    this.projectOpenshiftTestProjectPattern = projectOpenshiftTestProjectPattern;
  }

  public String getProjectOpenshiftDevProjectPattern() {
    return projectOpenshiftDevProjectPattern;
  }

  public void setProjectOpenshiftDevProjectPattern(String projectOpenshiftDevProjectPattern) {
    this.projectOpenshiftDevProjectPattern = projectOpenshiftDevProjectPattern;
  }

  public void setProjectOpenshiftCdProjectPattern(String projectOpenshiftCdProjectPattern) {
    this.projectOpenshiftCdProjectPattern = projectOpenshiftCdProjectPattern;
  }

  public void setProjectOpenshiftJenkinsProjectPattern(
      String projectOpenshiftJenkinsProjectPattern) {
    this.projectOpenshiftJenkinsProjectPattern = projectOpenshiftJenkinsProjectPattern;
  }

  public void setJenkinsPipelineProperties(JenkinsPipelineProperties jenkinsPipelineProperties) {
    this.jenkinsPipelineProperties = jenkinsPipelineProperties;
  }

  public String getBitbucketUri() {
    return bitbucketUri;
  }

  public void setBitbucketUri(String bitbucketUri) {
    this.bitbucketUri = bitbucketUri;
  }

  public String getBitbucketOdsProject() {
    return bitbucketOdsProject;
  }

  public void setBitbucketOdsProject(String bitbucketOdsProject) {
    this.bitbucketOdsProject = bitbucketOdsProject;
  }

  public String getOdsNamespace() {
    return odsNamespace;
  }

  public void setOdsNamespace(String odsNamespace) {
    this.odsNamespace = odsNamespace;
  }

  public String getOdsImageTag() {
    return odsImageTag;
  }

  public void setOdsImageTag(String odsImageTag) {
    this.odsImageTag = odsImageTag;
  }

  public String getOdsGitRef() {
    return odsGitRef;
  }

  public void setOdsGitRef(String odsGitRef) {
    this.odsGitRef = odsGitRef;
  }

  public void setQuickstarterJobs(List<Job> quickstarterJobs) {
    this.quickstarterJobs = quickstarterJobs;
  }

  public void setGeneralCdUser(String generalCdUser) {
    this.generalCdUser = generalCdUser;
  }

  public String getDefaultEntitlementGroups() {
    return defaultEntitlementGroups;
  }

  public void setDefaultEntitlementGroups(String defaultEntitlementGroups) {
    this.defaultEntitlementGroups = defaultEntitlementGroups;
  }

  public Map<String, Job> getNameToJobMappings() {
    return nameToJobMappings;
  }

  public Map<String, String> getLegacyComponentTypeToNameMappings() {
    return legacyComponentTypeToNameMappings;
  }

  public void setLegacyComponentTypeToNameMappings(
      Map<String, String> legacyComponentTypeToNameMappings) {
    this.legacyComponentTypeToNameMappings = legacyComponentTypeToNameMappings;
  }
}
