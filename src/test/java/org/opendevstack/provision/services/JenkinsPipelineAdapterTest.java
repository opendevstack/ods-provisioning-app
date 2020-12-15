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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.opendevstack.provision.config.Quickstarter.adminjobQuickstarter;
import static org.opendevstack.provision.config.Quickstarter.componentQuickstarter;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import java.io.IOException;
import java.util.*;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.config.JenkinsPipelineProperties;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jenkins.Execution;
import org.opendevstack.provision.model.jenkins.Job;
import org.opendevstack.provision.model.jenkins.Option;
import org.opendevstack.provision.model.webhookproxy.CreateProjectResponse;
import org.opendevstack.provision.util.CreateProjectResponseUtil;
import org.opendevstack.provision.util.ValueCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("utest")
public class JenkinsPipelineAdapterTest extends AbstractBaseServiceAdapterTest {

  private static final String PROJECT_KEY = "123key";

  @Autowired private JenkinsPipelineAdapter jenkinsPipelineAdapter;

  @Value("${jenkinspipeline.create-project.default-project-groups}")
  private String defaultEntitlementGroups;

  private final String JOB_1_NAME = "be-java-springboot";
  private final String JOB_1_REPO = "gitRepoName.git";
  private final String JOB_1_LEGACY_ID = "e5b77f0f-262a-42f9-9d06-5d9052c1f394";

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);

    jenkinsPipelineAdapter.jenkinsPipelineProperties = buildJenkinsPipelineProperties();

    jenkinsPipelineAdapter.groupPattern = "org.opendevstack.%s";
    jenkinsPipelineAdapter.adminWebhookProxyHost = "webhook-proxy-ods";
    jenkinsPipelineAdapter.projectWebhookProxyHostPattern = "webhook-proxy-%s-cd%s";
    jenkinsPipelineAdapter.projectOpenshiftJenkinsProjectPattern = "jenkins-%s-cd%s";
    jenkinsPipelineAdapter.projectOpenshiftBaseDomain = ".192.168.56.101.nip.io";
    jenkinsPipelineAdapter.projectOpenshiftCdProjectPattern = "%s/project/%s-cd";
    jenkinsPipelineAdapter.projectOpenshiftDevProjectPattern = "%s/project/%s-dev";
    jenkinsPipelineAdapter.projectOpenshiftConsoleUri = "https://192.168.56.101:8443/console";
    jenkinsPipelineAdapter.projectOpenshiftTestProjectPattern = "%s/project/%s-test";
    jenkinsPipelineAdapter.bitbucketUri = "http://192.168.56.31:7990";
    jenkinsPipelineAdapter.useTechnicalUser = true;
    jenkinsPipelineAdapter.userName = "maier";
    jenkinsPipelineAdapter.generalCdUser = "cd_user";
    jenkinsPipelineAdapter.odsNamespace = "ods";
    jenkinsPipelineAdapter.bitbucketOdsProject = "opendevstack";
    jenkinsPipelineAdapter.odsImageTag = "latest";
    jenkinsPipelineAdapter.odsGitRef = "master";
    jenkinsPipelineAdapter.init();
    super.beforeTest();
  }

  private JenkinsPipelineProperties buildJenkinsPipelineProperties() {
    JenkinsPipelineProperties jenkinsPipelineProperties = new JenkinsPipelineProperties();
    jenkinsPipelineProperties.addQuickstarter(
        adminjobQuickstarter(
            JenkinsPipelineAdapter.CREATE_PROJECTS_JOB_ID,
            "ods-core.git",
            "internal quickstarter for creating new initiatives",
            Optional.of("production"),
            Optional.of(JenkinsPipelineAdapter.CREATE_PROJECTS_JOB_ID + "/Jenkinsfile")));
    jenkinsPipelineProperties.addQuickstarter(
        componentQuickstarter(
            JOB_1_NAME, JOB_1_REPO, "dummy description", Optional.empty(), Optional.empty(), true));
    return jenkinsPipelineProperties;
  }

  @Test
  public void getQuickstarter() {
    JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);
    int expectedQuickstarterSize = jenkinsPipelineAdapter.getQuickstarterJobs().size();

    int actualQuickstarterSize = spyAdapter.getQuickstarterJobs().size();

    assertEquals(expectedQuickstarterSize, actualQuickstarterSize);
  }

  @Test
  public void executeJobsWhenQuickstartIsNull() throws Exception {
    JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);
    OpenProjectData project = new OpenProjectData();
    List<ExecutionsData> expectedExecutions = new ArrayList<>();

    ArrayList<Job> jobs = new ArrayList<>();
    mockJobsInServer(jobs);

    List<ExecutionsData> actualExecutions =
        spyAdapter.provisionComponentsBasedOnQuickstarters(project);

    assertEquals(expectedExecutions, actualExecutions);
  }

  @Test
  public void executeJobs() throws Exception {
    OpenProjectData project = new OpenProjectData();
    project.projectKey = PROJECT_KEY;
    project.webhookProxySecret = "secret101";
    String odsGitRef = "production";
    Job job = new Job(JOB_1_NAME, JOB_1_REPO, Optional.empty(), Optional.empty(), odsGitRef);

    Map<String, String> testjob = new HashMap<>();
    testjob.put(OpenProjectData.COMPONENT_ID_KEY, job.getId());
    testjob.put(OpenProjectData.COMPONENT_TYPE_KEY, job.getId());

    List<Map<String, String>> quickstart = new ArrayList<>();
    quickstart.add(testjob);
    project.quickstarters = quickstart;

    mockJobsInServer(asList(job));

    int expectedExecutionsSize = 1;
    ValueCaptor<Execution> bodyCaptor = new ValueCaptor<>();
    mockExecute(matchesClientCall().method(HttpMethod.POST).bodyCaptor(bodyCaptor))
        .thenReturn(buildDummyCreateProjectResponse());

    int actualExecutionsSize =
        jenkinsPipelineAdapter.provisionComponentsBasedOnQuickstarters(project).size();

    assertEquals(expectedExecutionsSize, actualExecutionsSize);
  }

  @Test
  public void testBuildExecutionUrl() {

    String webhookProxySecret = "secret101";
    String webhookHost = "localhost";

    String odsGitRef = "production";
    String componentId = "be-acc-service";
    Job job = new Job(JOB_1_NAME, JOB_1_REPO, Optional.empty(), Optional.empty(), odsGitRef);

    String expectedUrl =
        "https://"
            + webhookHost
            + "/build?trigger_secret="
            + webhookProxySecret
            + "&jenkinsfile_path="
            + job.jenkinsfilePath
            + "&component=ods-qs-"
            + componentId;

    String actualUrl =
        JenkinsPipelineAdapter.buildExecutionUrlQuickstarterJob(
            job, componentId, webhookProxySecret, webhookHost);

    assertEquals(expectedUrl, actualUrl);
  }

  @Test
  public void testBuildExecutionUrlAdminCreateProjectJob() {

    String webhookProxySecret = "secret101";
    String webhookHost = "localhost";

    String odsGitRef = "production";
    Job job =
        new Job(
            JenkinsPipelineAdapter.CREATE_PROJECTS_JOB_ID,
            JOB_1_REPO,
            Optional.empty(),
            Optional.empty(),
            odsGitRef);

    String componentId = "be-project-name-with-numbers-123";

    Map<String, String> testjob = new HashMap<>();
    testjob.put(OpenProjectData.COMPONENT_ID_KEY, componentId);
    testjob.put(OpenProjectData.COMPONENT_TYPE_KEY, job.getId());

    // Case: do not delete component job
    String expectedUrl =
        "https://"
            + webhookHost
            + "/build?trigger_secret="
            + webhookProxySecret
            + "&jenkinsfile_path="
            + job.jenkinsfilePath
            + "&component=ods-corejob-"
            + job.getId();

    String actualUrl =
        JenkinsPipelineAdapter.buildAdminJobExecutionUrl(
            job, componentId, job.getId(), webhookProxySecret, webhookHost, false);
    assertEquals(expectedUrl, actualUrl);
  }

  @Test
  public void testBuildExecutionUrlAdminDeleteProjectJob() {

    String webhookProxySecret = "secret101";
    String webhookHost = "localhost";

    String odsGitRef = "production";
    Job job =
        new Job(
            JenkinsPipelineAdapter.DELETE_PROJECTS_JOB_ID,
            JOB_1_REPO,
            Optional.empty(),
            Optional.empty(),
            odsGitRef);

    String projectId = "be-acc-service-with-123-numbers";

    Map<String, String> testjob = new HashMap<>();
    testjob.put(OpenProjectData.COMPONENT_ID_KEY, projectId);
    testjob.put(OpenProjectData.COMPONENT_TYPE_KEY, job.getId());

    // Case: delete component job
    String expectedUrl =
        "https://"
            + webhookHost
            + "/build?trigger_secret="
            + webhookProxySecret
            + "&jenkinsfile_path="
            + job.jenkinsfilePath
            + "&component=ods-corejob-"
            + projectId;

    String actualUrl =
        JenkinsPipelineAdapter.buildAdminJobExecutionUrl(
            job, projectId, job.getId(), webhookProxySecret, webhookHost, true);
    assertEquals(expectedUrl, actualUrl);
  }

  private void mockRestClientToReturnExecutionData(String output) throws java.io.IOException {
    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(output);
  }

  @Test
  public void createOpenshiftProjects() throws Exception {

    OpenProjectData projectData = createOpenProjectData("key");

    ValueCaptor<Execution> bodyCaptor = new ValueCaptor<>();
    mockExecute(matchesClientCall().method(HttpMethod.POST).bodyCaptor(bodyCaptor))
        .thenReturn(buildDummyCreateProjectResponse());

    OpenProjectData expectedOpenProjectData = generateDefaultOpenProjectData();

    OpenProjectData createdOpenProjectData =
        jenkinsPipelineAdapter.createPlatformProjects(projectData);

    Execution capturedBody = bodyCaptor.getValues().get(0);
    Assertions.assertEquals("production", capturedBody.branch);
    Assertions.assertEquals("ods-core", capturedBody.repository);
    List<Option> env = capturedBody.env;

    assertTrue(
        env.containsAll(
            List.of(
                new Option("PROJECT_ID", projectData.projectKey),
                new Option("PROJECT_ADMIN", jenkinsPipelineAdapter.userName),
                new Option("ODS_NAMESPACE", jenkinsPipelineAdapter.odsNamespace),
                new Option("ODS_BITBUCKET_PROJECT", jenkinsPipelineAdapter.bitbucketOdsProject),
                new Option("ODS_IMAGE_TAG", jenkinsPipelineAdapter.odsImageTag),
                new Option("ODS_GIT_REF", jenkinsPipelineAdapter.odsGitRef))));

    Assertions.assertEquals(
        jenkinsPipelineAdapter.bitbucketUri,
        capturedBody.getOptionValue(JenkinsPipelineAdapter.OPTION_KEY_GIT_SERVER_URL));

    assertEquals(expectedOpenProjectData, createdOpenProjectData);
    assertTrue(expectedOpenProjectData.platformRuntime);
    assertEquals(
        expectedOpenProjectData.platformCdEnvironmentUrl,
        createdOpenProjectData.platformCdEnvironmentUrl);
    assertEquals(
        expectedOpenProjectData.platformDevEnvironmentUrl,
        createdOpenProjectData.platformDevEnvironmentUrl);
    assertEquals(
        expectedOpenProjectData.platformTestEnvironmentUrl,
        createdOpenProjectData.platformTestEnvironmentUrl);
    assertEquals(
        expectedOpenProjectData.platformBuildEngineUrl,
        createdOpenProjectData.platformBuildEngineUrl);
  }

  @Test
  public void createOpenshiftProjectsFailWhenInvalidName() throws Exception {

    OpenProjectData projectData = createOpenProjectData("this.is.a.invalid.name-.");

    try {
      jenkinsPipelineAdapter.createPlatformProjects(projectData);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("is not valid name"));
    }
  }

  public OpenProjectData createOpenProjectData(String projectKey) throws IOException {
    OpenProjectData projectData = new OpenProjectData();
    projectData.projectKey = projectKey;
    projectData.webhookProxySecret = UUID.randomUUID().toString();

    Job job1 = new Job();
    job1.setName(JenkinsPipelineAdapter.CREATE_PROJECTS_JOB_ID);
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);

    mockJobsInServer(jobs);
    return projectData;
  }

  public void mockJobsInServer(List<Job> jobs) throws IOException {
    mockExecute(matchesClientCall().method(HttpMethod.GET)).thenReturn(jobs);
  }

  @Test
  public void createNullOCProject() {
    assertThrows(
        NullPointerException.class,
        () -> {
          JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);
          spyAdapter.createPlatformProjects(null);
        });
  }

  @Test
  public void createOpenshiftProjectsWithPassedAdminAndRoles() throws Exception {

    Job job1 = new Job();
    job1.setName(JenkinsPipelineAdapter.CREATE_PROJECTS_JOB_ID);
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);

    OpenProjectData projectData = new OpenProjectData();
    // create special permissionset - here crowd userdetails should never be called
    projectData.projectKey = "key";
    projectData.specialPermissionSet = true;
    projectData.projectAdminUser = "clemens";
    projectData.projectAdminGroup = "agroup";
    projectData.projectUserGroup = "ugroup";
    projectData.projectReadonlyGroup = "rgroup";
    projectData.webhookProxySecret = UUID.randomUUID().toString();

    mockJobsInServer(jobs);

    CreateProjectResponse response = buildDummyCreateProjectResponse();
    ValueCaptor<Object> valueHolder = new ValueCaptor<>();
    mockExecute(matchesClientCall().method(HttpMethod.POST).bodyCaptor(valueHolder))
        .thenReturn(response);

    jenkinsPipelineAdapter.createPlatformProjects(projectData);

    Execution actualBody = (Execution) valueHolder.getValues().get(0);
    assertNotNull(actualBody);
    assertEquals(projectData.projectAdminUser, actualBody.getOptionValue("PROJECT_ADMIN"));
    assertEquals(projectData.projectKey, actualBody.getOptionValue("PROJECT_ID"));
    assertEquals(
        jenkinsPipelineAdapter.bitbucketUri,
        actualBody.getOptionValue(JenkinsPipelineAdapter.OPTION_KEY_GIT_SERVER_URL));
    assertEquals(jenkinsPipelineAdapter.odsNamespace, actualBody.getOptionValue("ODS_NAMESPACE"));
    assertEquals(
        jenkinsPipelineAdapter.bitbucketOdsProject,
        actualBody.getOptionValue("ODS_BITBUCKET_PROJECT"));
    assertEquals(jenkinsPipelineAdapter.odsImageTag, actualBody.getOptionValue("ODS_IMAGE_TAG"));
    assertEquals(jenkinsPipelineAdapter.odsGitRef, actualBody.getOptionValue("ODS_GIT_REF"));

    String groups = actualBody.getOptionValue("PROJECT_GROUPS");
    assertNotNull(groups);
    assertTrue(groups.contains(defaultEntitlementGroups));
    assertTrue(groups.contains("ADMINGROUP=" + projectData.projectAdminGroup));
    assertTrue(groups.contains("USERGROUP=" + projectData.projectUserGroup));
    assertTrue(groups.contains("READONLYGROUP=" + projectData.projectReadonlyGroup));
  }

  private CreateProjectResponse buildDummyCreateProjectResponse() {
    return CreateProjectResponseUtil.buildDummyCreateProjectResponse(
        "demo-cd", "demo-cd-build-config", 1);
  }

  @Test
  public void getEndpointAPIPath() {
    NotImplementedException ex =
        assertThrows(
            NotImplementedException.class, () -> jenkinsPipelineAdapter.getAdapterApiUri());
    String expectedMessage = "JenkinsPipelineAdapter#getAdapterApiUri";
    String actualMessage = ex.getMessage();
    assertTrue(actualMessage.contains(expectedMessage));
  }

  private OpenProjectData generateDefaultOpenProjectData() {
    OpenProjectData result = new OpenProjectData();
    result.platformCdEnvironmentUrl = "https://192.168.56.101:8443/console/project/key-cd";
    result.platformDevEnvironmentUrl = "https://192.168.56.101:8443/console/project/key-dev";
    result.platformTestEnvironmentUrl = "https://192.168.56.101:8443/console/project/key-test";
    result.platformBuildEngineUrl = "https://jenkins-key-cd.192.168.56.101.nip.io";
    result.bugtrackerSpace = true;
    result.platformRuntime = true;
    result.projectKey = "key";
    return result;
  }

  @Test
  public void getComponentByLegacyComponentType() {
    Optional<Job> foundByLegacyType = jenkinsPipelineAdapter.getComponentByType(JOB_1_LEGACY_ID);
    assertTrue(foundByLegacyType.isPresent());
  }

  @Test
  public void getComponentByNameComponentType() {
    Optional<Job> foundByLegacyType = jenkinsPipelineAdapter.getComponentByType(JOB_1_NAME);
    assertTrue(foundByLegacyType.isPresent());
  }

  @Test
  public void appendToMapEntry() {}

  @Test
  public void addOrAppendToMapEntryLambda() {

    Map<String, String> map = new HashMap<>();
    map.put("k1", "v1");
    map.put("k2", "v2");

    String key = "k1";
    JenkinsPipelineAdapter.addOrAppendToEntry(map, key, "appended", ",");
    assertEquals("v1,appended", map.get(key));

    key = "newKey";
    String value = "newValue";
    JenkinsPipelineAdapter.addOrAppendToEntry(map, key, value, ",");
    assertEquals(value, map.get(key));
  }
}
