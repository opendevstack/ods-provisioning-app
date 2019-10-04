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

package org.opendevstack.provision.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.JiraServer;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.model.confluence.SpaceData;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.storage.LocalStorage;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.RundeckJobStore;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientMockHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * End to end testcase with real result data - only mock is the RestClient - to feed the json
 *
 * @author utschig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBoot.class)
@DirtiesContext
public class E2EProjectAPIControllerTest {
  private static Logger e2eLogger = LoggerFactory.getLogger(E2EProjectAPIControllerTest.class);

  @InjectMocks @Autowired private JiraAdapter realJiraAdapter;

  @InjectMocks @Autowired private ConfluenceAdapter realConfluenceAdapter;

  @InjectMocks @Autowired private BitbucketAdapter realBitbucketAdapter;

  @InjectMocks @Autowired private RundeckAdapter realRundeckAdapter;

  @Mock RestClient restClient;
  RestClientMockHelper mockHelper;

  @InjectMocks @Autowired private ProjectApiController apiController;

  @Autowired private LocalStorage realStorageAdapter;

  @Autowired private MailAdapter realMailAdapter;

  @Autowired private RundeckJobStore realJobStore;

  @Value("${idmanager.group.opendevstack-users}")
  private String userGroup;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String adminGroup;

  private MockMvc mockMvc;

  // directory containing all the e2e test data
  private static File testDataDir = new File("src/test/resources/e2e/");

  // results directory
  private static File resultsDir = new File(testDataDir, "results");

  // do NOT delete on cleanup
  private static List<String> excludeFromCleanup =
      Arrays.asList("20190101171023-LEGPROJ.txt", "20190101171024-CRACKED.txt");
  private MockHttpSession mocksession;

  @Before
  public void setUp() throws Exception {
    cleanUp();
    mocksession = new MockHttpSession();
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    mockHelper = new RestClientMockHelper(restClient);
    // setup storage against test directory
    realStorageAdapter.setLocalStoragePath(resultsDir.getPath());

    // disable mail magic
    realMailAdapter.isMailEnabled = false;

    // populate the rundeck jobs
    List<Job> jobList =
        readTestDataTypeRef("rundeck-get-jobs-response", new TypeReference<List<Job>>() {});

    realJobStore.addJobs(jobList);
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    for (File fresult : resultsDir.listFiles()) {
      if (fresult.isDirectory() || excludeFromCleanup.contains(fresult.getName())) {
        continue;
      }
      e2eLogger.debug("Deleting file {} result: {}", fresult.getName(), fresult.delete());
    }
  }

  /** Test positive - e2e new project - no quickstarters */
  @Test
  public void testProvisionNewSimpleProjectE2E() throws Exception {
    testProvisionNewSimpleProjectInternal(false);
  }

  /**
   * Test negative - e2e new project - no quickstarters, rollback any external changes - bugtracker,
   * scm,...
   */
  @Test
  public void testProvisionNewSimpleProjectE2EFail() throws Exception {
    cleanUp();
    testProvisionNewSimpleProjectInternal(true);
  }

  /** Test negative - e2e new project - no quickstarters, but NO cleanup allowed :) */
  @Test
  public void testProvisionNewSimpleProjectE2EFailCleanupNotAllowed() throws Exception {
    cleanUp();
    apiController.cleanupAllowed = false;
    testProvisionNewSimpleProjectInternal(true);
  }

  public void testProvisionNewSimpleProjectInternal(boolean fail) throws Exception {
    // read the request
    OpenProjectData data = readTestData("ods-create-project-request", OpenProjectData.class);

    // jira server create project response
    LeanJiraProject jiraProject =
        readTestData("jira-create-project-response", LeanJiraProject.class);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realJiraAdapter.getAdapterApiUri() + "/project"))
                .method(HttpMethod.POST))
        .thenReturn(jiraProject);
    // get confluence blueprints
    List<Blueprint> blList =
        readTestDataTypeRef(
            "confluence-get-blueprints-response", new TypeReference<List<Blueprint>>() {});

    mockHelper
        .mockExecute(
            matchesClientCall().url(containsString("dialog/web-items")).method(HttpMethod.GET))
        .thenReturn(blList);

    // get jira servers for confluence space
    List<JiraServer> jiraservers =
        readTestDataTypeRef(
            "confluence-get-jira-servers-response", new TypeReference<List<JiraServer>>() {});

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString("jiraanywhere/1.0/servers"))
                .method(HttpMethod.GET))
        .thenReturn(jiraservers);

    SpaceData confluenceSpace = readTestData("confluence-create-space-response", SpaceData.class);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString("space-blueprint/create-space"))
                .bodyMatches(instanceOf(Space.class))
                .method(HttpMethod.POST))
        .thenReturn(confluenceSpace);

    // bitbucket main project creation
    BitbucketProjectData bitbucketProjectData =
        readTestData("bitbucket-create-project-response", BitbucketProjectData.class);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realBitbucketAdapter.getAdapterApiUri()))
                .bodyMatches(instanceOf(BitbucketProject.class))
                .method(HttpMethod.POST)
                .returnType(BitbucketProjectData.class))
        .thenReturn(bitbucketProjectData);

    // bitbucket aus repo creation - oc-config
    RepositoryData bitbucketRepositoryDataOCConfig =
        readTestData("bitbucket-create-repo-occonfig-response", RepositoryData.class);
    Repository occonfigRepo = new Repository();
    occonfigRepo.setName(bitbucketRepositoryDataOCConfig.getName());
    occonfigRepo.setUserGroup(userGroup);
    occonfigRepo.setAdminGroup(adminGroup);

    // bitbucket aux repo creation - design repo
    RepositoryData bitbucketRepositoryDataDesign =
        readTestData("bitbucket-create-repo-design-response", RepositoryData.class);
    Repository designRepo = new Repository();
    designRepo.setName(bitbucketRepositoryDataDesign.getName());
    designRepo.setUserGroup(userGroup);
    designRepo.setAdminGroup(adminGroup);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realBitbucketAdapter.getAdapterApiUri() + "/TESTP/repos"))
                .method(HttpMethod.POST)
                .returnType(RepositoryData.class))
        .thenReturn(bitbucketRepositoryDataOCConfig, bitbucketRepositoryDataDesign);

    // will cause cleanup
    String rundeckUrl = realRundeckAdapter.getAdapterApiUri() + "/project/";
    if (fail) {
      mockHelper
          .mockExecute(
              matchesClientCall()
                  .url(containsString(rundeckUrl))
                  .bodyMatches(instanceOf(Map.class))
                  .method(HttpMethod.GET))
          .thenThrow(new IOException("Rundeck TestFail"));
    } else {
      mockRundeckDefaultJobs();
    }

    // rundeck create-projects job execution
    ExecutionsData execution =
        readTestData("rundeck-create-project-response", ExecutionsData.class);

    String createJobId = realJobStore.getJobIdForJobName("create-projects");
    assertNotNull(createJobId);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(createRundeckJobPath(createJobId)))
                .bodyMatches(instanceOf(Execution.class))
                .method(HttpMethod.POST))
        .thenReturn(execution);
    // create the ODS project
    MvcResult resultProjectCreationResponse =
        mockMvc
            .perform(
                post("/api/v2/project")
                    .content(ProjectApiControllerTest.asJsonString(data))
                    .contentType(MediaType.APPLICATION_JSON)
                    .session(mocksession)
                    .accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

    if (!fail) {
      assertEquals(
          MockHttpServletResponse.SC_OK, resultProjectCreationResponse.getResponse().getStatus());
    } else {
      assertEquals(
          MockHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          resultProjectCreationResponse.getResponse().getStatus());

      // no cleanup happening - so no delete calls
      if (!apiController.cleanupAllowed) {
        // 5 delete calls, jira / confluence / bitbucket project and two repos
        //        Mockito.verify(mockOldRestClient, times(0))
        //            .callHttp(anyString(), eq(null), anyBoolean(), eq(HttpVerb.DELETE), eq(null));
        mockHelper.verifyExecute(matchesClientCall().method(HttpMethod.DELETE), never());
        return;
      }

      // 5 delete calls, jira / confluence / bitbucket project and two repos
      mockHelper.verifyExecute(matchesClientCall().method(HttpMethod.DELETE), times(5));

      // delete jira project
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realJiraAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(1));

      // delete confluence space
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realConfluenceAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(1));

      // delete repos and bitbucket project
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realBitbucketAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(3));
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(
                  allOf(
                      containsString(realBitbucketAdapter.getAdapterApiUri()),
                      containsString("repos")))
              .method(HttpMethod.DELETE),
          times(2));
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realConfluenceAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(1));
      return;
    }

    // get the project thru its key
    MvcResult resultProjectGetResponse =
        mockMvc
            .perform(
                get("/api/v2/project/" + data.projectKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .session(mocksession))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

    // verify responses
    assertEquals(
        resultProjectCreationResponse.getResponse().getContentAsString(),
        resultProjectGetResponse.getResponse().getContentAsString());

    OpenProjectData resultProject =
        new ObjectMapper()
            .readValue(
                resultProjectGetResponse.getResponse().getContentAsString(), OpenProjectData.class);

    // verify the execution
    assertEquals(1, resultProject.lastExecutionJobs.size());
    assertEquals(execution.getPermalink(), resultProject.lastExecutionJobs.iterator().next());

    // verify 2 repos are created
    assertEquals("Repository created", 2, resultProject.repositories.size());
  }

  public void mockRundeckDefaultJobs() throws Exception {
    List<Job> jobList =
        readTestDataTypeRef("rundeck-get-jobs-response", new TypeReference<List<Job>>() {});
    mockRundeckJobs(jobList);
  }

  public void mockRundeckJobs(List<Job> jobList) throws IOException {
    Map<String, List<Job>> groups =
        jobList.stream().collect(Collectors.groupingBy(j -> j.getGroup()));
    for (Entry<String, List<Job>> jobGrouped : groups.entrySet()) {
      List<Job> value = jobGrouped.getValue();
      String key = jobGrouped.getKey();
      mockHelper
          .mockExecute(
              matchesClientCall()
                  .url(
                      containsString(
                          (realRundeckAdapter.getAdapterApiUri() + "/project/")
                              + "Quickstarters/jobs"))
                  .queryParam("groupPath", key)
                  .method(HttpMethod.GET))
          .thenReturn(value);
    }
    realJobStore.addJobs(jobList);
  }

  /** Test positive new quickstarter */
  @Test
  public void testQuickstarterProvisionOnNewOpenProject() throws Exception {
    testQuickstarterProvisionOnNewOpenProject(false);
  }

  /** Test positive new quickstarter and delete project afterwards */
  @Test
  public void testQuickstarterProvisionOnNewOpenProjectInclDelete() throws Exception {
    mockRundeckDefaultJobs();
    OpenProjectData createdProjectIncludingQuickstarters =
        testQuickstarterProvisionOnNewOpenProject(false);

    assertNotNull(createdProjectIncludingQuickstarters);
    assertNotNull(createdProjectIncludingQuickstarters.projectKey);
    assertNotNull(createdProjectIncludingQuickstarters.quickstarters);
    assertEquals(1, createdProjectIncludingQuickstarters.quickstarters.size());

    OpenProjectData toClean = new OpenProjectData();
    toClean.projectKey = createdProjectIncludingQuickstarters.projectKey;
    toClean.quickstarters = createdProjectIncludingQuickstarters.quickstarters;

    // delete the quickstarter IN the project
    mockMvc
        .perform(
            delete("/api/v2/project/")
                .content(ProjectApiControllerTest.asJsonString(toClean))
                .contentType(MediaType.APPLICATION_JSON)
                .session(mocksession)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk());

    String deleteComponentJobId =
        realJobStore.getJobIdForJobName(RundeckAdapter.DELETE_COMPONENT_JOB);
    assertNotNull(deleteComponentJobId);

    // delete component thru rundeck

    mockHelper.verifyExecute(
        matchesClientCall()
            .url(containsString(createRundeckJobPath(deleteComponentJobId)))
            .bodyMatches(instanceOf(Execution.class))
            .method(HttpMethod.POST),
        times(1));

    // delete the ODS project
    mockMvc
        .perform(
            delete("/api/v2/project/" + toClean.projectKey)
                .contentType(MediaType.APPLICATION_JSON)
                .session(mocksession)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk());

    String deleteProjectJobId = realJobStore.getJobIdForJobName(RundeckAdapter.DELETE_PROJECTS_JOB);
    assertNotNull(deleteProjectJobId);

    // delete projects rundeck job
    RestClientCallArgumentMatcher wantedArgument =
        matchesClientCall()
            .url(containsString(createRundeckJobPath(deleteComponentJobId)))
            .bodyMatches(instanceOf(Execution.class))
            .method(HttpMethod.POST);
    mockHelper.verifyExecute(
        matchesClientCall()
            .url(containsString(createRundeckJobPath(deleteProjectJobId)))
            .bodyMatches(instanceOf(Execution.class))
            .method(HttpMethod.POST),
        times(1));
  }

  /** Test NEGATIVE new quickstarter - rollback ONE created repo */
  @Test
  public void testQuickstarterProvisionOnNewOpenProjectFail() throws Exception {
    testQuickstarterProvisionOnNewOpenProject(true);
  }

  public OpenProjectData testQuickstarterProvisionOnNewOpenProject(boolean fail) throws Exception {
    // read the request
    OpenProjectData dataUpdate =
        readTestData("ods-update-project-python-qs-request", OpenProjectData.class);

    // if project does not exist, create it thru the test
    if (realStorageAdapter.getProject(dataUpdate.projectKey) == null) {
      testProvisionNewSimpleProjectE2E();
    }

    OpenProjectData currentlyStoredProject = realStorageAdapter.getProject(dataUpdate.projectKey);

    assertNull(currentlyStoredProject.quickstarters);

    // bitbucket repo creation for new quickstarter
    RepositoryData bitbucketRepositoryDataQSRepo =
        readTestData("bitbucket-create-repo-python-qs-response", RepositoryData.class);

    Repository qsrepo = new Repository();
    qsrepo.setName(bitbucketRepositoryDataQSRepo.getName());
    qsrepo.setUserGroup(userGroup);
    qsrepo.setAdminGroup(adminGroup);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realBitbucketAdapter.getAdapterApiUri()))
                .method(HttpMethod.POST))
        .thenReturn(bitbucketRepositoryDataQSRepo);

    // get the rundeck jobs
    List<Job> jobList =
        readTestDataTypeRef("rundeck-get-jobs-response", new TypeReference<List<Job>>() {});
    jobList.stream().forEach(job -> job.setGroup("quickstarts"));

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realRundeckAdapter.getAdapterApiUri() + "/project/"))
                .method(HttpMethod.GET))
        .thenReturn(jobList);
    // rundeck create component job execution
    ExecutionsData execution =
        readTestData("rundeck-create-python-qs-response", ExecutionsData.class);

    String createPythonComponentQSJob = realJobStore.getJobIdForJobName("create-rshiny");
    assertNotNull(createPythonComponentQSJob);

    // if !fail - return a clean response from rundeck, else let the execution post fail
    if (!fail) {
      mockHelper
          .mockExecute(
              matchesClientCall()
                  .url(containsString(createRundeckJobPath(createPythonComponentQSJob)))
                  .bodyMatches(instanceOf(Execution.class))
                  .method(HttpMethod.POST))
          .thenReturn(execution);
    } else {
      mockHelper
          .mockExecute(
              matchesClientCall()
                  .url(containsString(createRundeckJobPath(createPythonComponentQSJob)))
                  .bodyMatches(instanceOf(Execution.class))
                  .method(HttpMethod.POST))
          .thenThrow(new IOException("Rundeck provision job failed"));
    }

    // update the project with the new quickstarter
    MvcResult resultUpdateResponse =
        mockMvc
            .perform(
                put("/api/v2/project")
                    .content(ProjectApiControllerTest.asJsonString(dataUpdate))
                    .contentType(MediaType.APPLICATION_JSON)
                    .session(mocksession)
                    .accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

    if (fail) {
      assertEquals(
          MockHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          resultUpdateResponse.getResponse().getStatus());

      // delete repository
      VerificationMode times = times(1);
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realBitbucketAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times);

      // verify project(s) are untouched

      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realJiraAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(0));

      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realConfluenceAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(0));
      return dataUpdate;
    } else {
      assertEquals(MockHttpServletResponse.SC_OK, resultUpdateResponse.getResponse().getStatus());
    }

    // get the inlined body result
    String resultUpdateData = resultUpdateResponse.getResponse().getContentAsString();
    assertNotNull(resultUpdateData);

    // convert into a project pojo
    OpenProjectData resultProject =
        new ObjectMapper().readValue(resultUpdateData, OpenProjectData.class);

    List<Map<String, String>> createdQuickstarters = resultProject.quickstarters;

    assertNotNull(createdQuickstarters);
    assertEquals(1, createdQuickstarters.size());

    assertEquals(1, resultProject.lastExecutionJobs.size());
    assertEquals(execution.getPermalink(), resultProject.lastExecutionJobs.iterator().next());

    // return the new fully built project for further use
    return resultProject;
  }

  public void oldVerify(VerificationMode times, RestClientCallArgumentMatcher wantedArgument)
      throws IOException {
    mockHelper.verifyExecute(wantedArgument, times);
  }

  /** Test legacy upgrade e2e */
  @Test
  public void testLegacyProjectUpgradeOnGet() throws Exception {
    // populate the rundeck jobs
    mockRundeckDefaultJobs();
    // get the project thru its key
    MvcResult resultLegacyProjectGetResponse =
        mockMvc
            .perform(
                get("/api/v2/project/LEGPROJ")
                    .accept(MediaType.APPLICATION_JSON)
                    .session(mocksession))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

    OpenProjectData resultLegacyProject =
        new ObjectMapper()
            .readValue(
                resultLegacyProjectGetResponse.getResponse().getContentAsString(),
                OpenProjectData.class);

    // verify 4 repos are there - 2 base 2 qs
    assertEquals(4, resultLegacyProject.repositories.size());

    // verify 2 quickstarters are there
    assertEquals(2, resultLegacyProject.quickstarters.size());
  }

  /*
   * internal test helpers
   */

  private <T> T readTestData(String name, Class<T> returnType) throws Exception {
    return new ObjectMapper().readValue(findTestFile(name), returnType);
  }

  private <T> T readTestDataTypeRef(String name, TypeReference<T> returnType) throws Exception {
    return new ObjectMapper().readValue(findTestFile(name), returnType);
  }

  private File findTestFile(String fileName) throws IOException {
    Preconditions.checkNotNull(fileName, "File cannot be null");
    if (!fileName.endsWith(".json")) {
      fileName = fileName + ".json";
    }
    File dataFile = new File(testDataDir, fileName);
    if (!dataFile.exists()) {
      throw new IOException("Cannot find testfile with name:" + dataFile.getName());
    }
    return dataFile;
  }

  private String createRundeckJobPath(String jobId) {
    Preconditions.checkNotNull(jobId, "job id cannot be null");
    return String.format("job/%s/run", jobId);
  }
}
