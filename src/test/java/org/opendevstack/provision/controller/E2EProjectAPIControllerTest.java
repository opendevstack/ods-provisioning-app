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

import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.authentication.basic.BasicAuthSecurityTestConfig;
import org.opendevstack.provision.model.ExecutionJob;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.JiraServer;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.model.confluence.SpaceData;
import org.opendevstack.provision.model.jenkins.Execution;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.model.jira.PermissionSchemeResponse;
import org.opendevstack.provision.model.webhookproxy.CreateProjectResponse;
import org.opendevstack.provision.services.*;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.CrowdProjectIdentityMgmtAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.storage.LocalStorage;
import org.opendevstack.provision.util.CreateProjectResponseUtil;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientMockHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End to end testcase with real result data - only mock is the RestClient - to feed the json
 *
 * @author utschig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SpringBoot.class)
@ActiveProfiles("utest")
public class E2EProjectAPIControllerTest {

  private static Logger e2eLogger = LoggerFactory.getLogger(E2EProjectAPIControllerTest.class);

  public static final String BITBUCKET_PARAM_FILTER = "filter";
  public static final String BITBUCKET_PARAM_PERMISSION = "permission";

  public static final String testAdminUsername = "testUserName";
  public static final String TEST_VALID_CREDENTIAL =
      BasicAuthSecurityTestConfig.TEST_VALID_CREDENTIAL;

  private RestClientMockHelper mockHelper;

  private MockMvc mockMvc;

  @MockBean private IODSAuthnzAdapter mockAuthnzAdapter;

  @MockBean private RestClient restClient;

  @MockBean private CrowdProjectIdentityMgmtAdapter crowdProjectIdentityMgmtAdapter;

  @Autowired private WebApplicationContext context;

  @Autowired private JiraAdapter realJiraAdapter;

  @Autowired private ConfluenceAdapter realConfluenceAdapter;

  @Autowired private BitbucketAdapter realBitbucketAdapter;

  @Autowired private ProjectApiController apiController;

  @Autowired private LocalStorage realLocalStorageAdapter;

  @Autowired private MailAdapter realMailAdapter;

  @Autowired private TestRestTemplate template;

  @Qualifier("testUsersAndRoles")
  @Autowired
  private Map<String, String> testUsersAndRoles;

  @Value("${idmanager.group.opendevstack-users}")
  private String userGroup;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String adminGroup;

  private static TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  private static File testProjectDataDir =
      new File(TestDataFileReader.TEST_DATA_FILE_DIR, "results");

  private static File buildDir = new File("./build");

  private String VALID_CREDENTIAL = BasicAuthSecurityTestConfig.TEST_VALID_CREDENTIAL;

  @Before
  public void setUp() {

    testUsersAndRoles.put(testAdminUsername, adminGroup);

    E2EProjectAPIControllerTest.initLocalStorage(
        realLocalStorageAdapter, E2EProjectAPIControllerTest.createTempDir(buildDir));

    apiController.setDirectStorage(realLocalStorageAdapter);

    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

    mockHelper = new RestClientMockHelper(restClient);

    // disable mail magic
    realMailAdapter.isMailEnabled = false;

    // override configuration in application.properties, some tests depends on cleanupAllowed
    apiController.setCleanupAllowed(true);

    when(mockAuthnzAdapter.getUserName()).thenReturn(testAdminUsername);
    when(mockAuthnzAdapter.getUserPassword()).thenReturn(TEST_VALID_CREDENTIAL);
  }

  public static void initLocalStorage(LocalStorage realStorageAdapter, File resultsDir) {
    Assert.assertNotNull(realStorageAdapter.getLocalStoragePath());
    Assert.assertTrue(resultsDir.exists());
    Assert.assertTrue(resultsDir.isDirectory());
    realStorageAdapter.setLocalStoragePath(resultsDir.getPath());
    Assert.assertNotNull(realStorageAdapter.getLocalStoragePath());
    e2eLogger.info(
        "Local storage initialized. LocalStoragePath={}", realStorageAdapter.getStoragePath());
  }

  @After
  public void cleanUpTempDir() {
    new File(realLocalStorageAdapter.getLocalStoragePath()).deleteOnExit();
  }

  /** Test positive - e2e new project - no quickstarters */
  @Test
  public void testProvisionNewSimpleProjectE2E() throws Exception {
    testProvisionNewSimpleProjectInternal(false, false);
  }

  /**
   * Test negative - e2e new project - no quickstarters, rollback any external changes - bugtracker,
   * scm,...
   */
  @Test
  public void testProvisionNewSimpleProjectE2EFail() throws Exception {
    testProvisionNewSimpleProjectInternal(true, false);
  }

  /**
   * Test negative - e2e new project, with perm set - no quickstarters, rollback any external
   * changes - bugtracker, scm,... and also permission set
   */
  @Test
  public void testProvisionNewSimplePermsetProjectE2EFail() throws Exception {
    testProvisionNewSimpleProjectInternal(true, true);
  }

  /** Test negative - e2e new project - no quickstarters, but NO cleanup allowed :) */
  @Test
  public void testProvisionNewSimpleProjectE2EFailCleanupNotAllowed() throws Exception {
    apiController.setCleanupAllowed(false);
    testProvisionNewSimpleProjectInternal(true, false);
  }

  public void testProvisionNewSimpleProjectInternal(boolean fail, boolean specialPermissionSet)
      throws Exception {
    // read the request
    OpenProjectData data = readTestData("ods-create-project-request", OpenProjectData.class);

    data.specialPermissionSet = specialPermissionSet;

    // jira server create project response
    LeanJiraProject jiraProject =
        readTestData("jira-create-project-response", LeanJiraProject.class);

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(realJiraAdapter.getAdapterApiUri() + "/project"))
                .method(HttpMethod.POST))
        .thenReturn(jiraProject);

    // jira server pre conditions
    String getUserResponse = fileReader.readFileContent("jira-get-user-template");
    if (data.specialPermissionSet && data.projectAdminUser != null) {
      getUserResponse = getUserResponse.replace("<%USERNAME%>", data.projectAdminUser);
    } else {
      getUserResponse = getUserResponse.replace("<%USERNAME%>", testAdminUsername);
    }

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(JiraAdapter.JIRA_API_USERS))
                .method(HttpMethod.GET))
        .thenReturn(getUserResponse);

    String getUserMyPermissionsResponse =
        fileReader
            .readFileContent(JiraAdapterTests.TEST_DATA_FILE_JIRA_GET_MYPERMISSIONS_TEMPLATE)
            .replace("<%HAVE_PERMISSION%>", "true");

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(JiraAdapter.JIRA_API_MYPERMISSIONS))
                .method(HttpMethod.GET))
        .thenReturn(getUserMyPermissionsResponse);

    String getGroup = fileReader.readFileContent("jira-get-group-template");
    HashSet<String> groups = new HashSet<>();
    if (data.specialPermissionSet && data.projectAdminUser != null) {
      groups.addAll(data.specialPermissionSetGroups());
    }

    groups.add(adminGroup);
    groups.add(userGroup);

    groups.forEach(
        group -> {
          try {
            mockHelper
                .mockExecute(
                    matchesClientCall()
                        .url(containsString(JiraAdapter.JIRA_API_GROUPS_PICKER))
                        .queryParam("query", group)
                        .method(HttpMethod.GET))
                .thenReturn(getGroup.replace("<%GROUP%>", group));

          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    // jira server find & create permission scheme
    PermissionSchemeResponse jiraProjectPermSet =
        readTestData("jira-get-project-permissionsscheme", PermissionSchemeResponse.class);

    mockHelper
        .mockExecute(
            matchesClientCall().url(containsString("/permissionscheme")).method(HttpMethod.GET))
        .thenReturn(jiraProjectPermSet);

    PermissionScheme jiraProjectPermSetCreate =
        readTestData("jira-get-project-permissionsscheme", PermissionScheme.class);

    mockHelper
        .mockExecute(
            matchesClientCall().url(containsString("/permissionscheme")).method(HttpMethod.POST))
        .thenReturn(jiraProjectPermSetCreate);

    // get confluence blueprints
    List<Blueprint> blList =
        readTestDataTypeRef(
            "confluence-get-blueprints-response", new TypeReference<List<Blueprint>>() {});

    mockHelper
        .mockExecute(
            matchesClientCall().url(containsString("dialog/web-items")).method(HttpMethod.GET))
        .thenReturn(blList);

    String confluenceUserTemplate = fileReader.readFileContent("confluence-get-user-template");
    HashSet<String> confluenceUsers = new HashSet<>();
    confluenceUsers.add(data.projectAdminUser);
    confluenceUsers.add(testAdminUsername);
    confluenceUsers.forEach(
        username -> {
          try {
            mockHelper
                .mockExecute(
                    matchesClientCall()
                        .url(
                            containsString(
                                realConfluenceAdapter.getAdapterApiUri()
                                    + "/"
                                    + ConfluenceAdapter.CONFLUENCE_API_USER))
                        .queryParam(ConfluenceAdapter.USERNAME_PARAM, username)
                        .method(HttpMethod.GET))
                .thenReturn(confluenceUserTemplate.replace("<%USERNAME%>", username));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    String confluenceGroupTemplate = fileReader.readFileContent("confluence-get-group-template");
    HashSet<String> confluenceGroups = new HashSet<>();
    confluenceGroups.add(data.projectAdminGroup);
    confluenceGroups.add(data.projectReadonlyGroup);
    confluenceGroups.add(data.projectUserGroup);
    confluenceGroups.add(adminGroup);
    confluenceGroups.add(userGroup);

    confluenceGroups.forEach(
        group -> {
          try {
            mockHelper
                .mockExecute(
                    matchesClientCall()
                        .url(
                            containsString(
                                realConfluenceAdapter.getAdapterApiUri()
                                    + "/"
                                    + ConfluenceAdapter.CONFLUENCE_API_GROUP
                                    + "/"
                                    + group))
                        .method(HttpMethod.GET))
                .thenReturn(confluenceGroupTemplate.replace("<%GROUP%>", group));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

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

    // bitbucket pre conditions checks
    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(
                    containsString(
                        realBitbucketAdapter.getAdapterRootApiUri()
                            + "/"
                            + BitbucketAdapter.BITBUCKET_API_USERS))
                .queryParam(BITBUCKET_PARAM_FILTER, testAdminUsername)
                .queryParam(
                    BITBUCKET_PARAM_PERMISSION, BitbucketAdapter.GLOBAL_PERMISSION_PROJECT_CREATE)
                .method(HttpMethod.GET))
        .thenReturn(
            fileReader.readFileContent("bitbucket-get-user-permission-project-create-response"));

    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(
                    containsString(
                        realBitbucketAdapter.getAdapterRootApiUri()
                            + "/"
                            + BitbucketAdapter.BITBUCKET_API_ADMIN_USERS))
                .queryParam(BITBUCKET_PARAM_FILTER, "cd_user")
                .method(HttpMethod.GET))
        .thenReturn(fileReader.readFileContent("bitbucket-get-admin-user-response"));

    List.of(
            "opendevstack-administrators",
            "opendevstack-users",
            "BI-AS-ATLASSIAN-P-TestP-STAKEHOLDER",
            "BI-AS-ATLASSIAN-P-TestP-MANAGER",
            "BI-AS-ATLASSIAN-P-TestP-TEAM")
        .forEach(
            groupName -> {
              try {
                String template =
                    fileReader.readFileContent("bitbucket-get-group-response-template");
                mockHelper
                    .mockExecute(
                        matchesClientCall()
                            .url(
                                containsString(
                                    realBitbucketAdapter.getAdapterRootApiUri()
                                        + "/"
                                        + BitbucketAdapter.BITBUCKET_API_GROUPS))
                            .queryParam(BITBUCKET_PARAM_FILTER, groupName)
                            .method(HttpMethod.GET))
                    .thenReturn(template.replace("%GROUP_NAME%", groupName));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

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

    // verify jira components for new repos NOT created (just 2 auxiliaries)
    mockHelper.verifyExecute(
        matchesClientCall()
            .url(containsString(realJiraAdapter.getAdapterApiUri() + "/component"))
            .method(HttpMethod.POST),
        times(0));

    CreateProjectResponse configuredResponse =
        CreateProjectResponseUtil.buildDummyCreateProjectResponse("demo-cd", "build-config", 1);
    // will cause cleanup
    OngoingStubbing<Object> stub =
        mockHelper.mockExecute(
            matchesClientCall()
                .url(
                    containsString(
                        createJenkinsJobPath(
                            "prov",
                            "create-projects/Jenkinsfile",
                            "ods-corejob-" + data.projectKey.toLowerCase())))
                .bodyMatches(instanceOf(Execution.class))
                .method(HttpMethod.POST));
    if (fail) {
      stub.thenThrow(new IOException("Jenkins TestFail"));
    } else {
      stub.thenReturn(configuredResponse);
    }

    // create the ODS project
    MvcResult resultProjectCreationResponse =
        mockMvc
            .perform(
                post("/api/v2/project")
                    .content(ProjectApiControllerTest.asJsonString(data))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(httpBasic(testAdminUsername, VALID_CREDENTIAL))
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

      assertTrue(
          resultProjectCreationResponse
              .getResponse()
              .getContentAsString()
              .contains(
                  "An error occured while creating project [TESTP], reason [Jenkins TestFail] - but all cleaned up!"));

      // no cleanup happening - so no delete calls
      if (!apiController.isCleanupAllowed()) {
        // 5 delete calls, jira / confluence / bitbucket project and two repos
        //        Mockito.verify(mockOldRestClient, times(0))
        //            .callHttp(anyString(), eq(null), anyBoolean(), eq(HttpVerb.DELETE), eq(null));
        mockHelper.verifyExecute(matchesClientCall().method(HttpMethod.DELETE), never());
        return;
      }

      // 5 delete calls, jira / confluence / bitbucket project and two repos, or 6 if permission set
      int overallDeleteCalls = specialPermissionSet ? 6 : 5;
      mockHelper.verifyExecute(
          matchesClientCall().method(HttpMethod.DELETE), times(overallDeleteCalls));

      // delete jira project (and potentially permission set)
      int jiraDeleteCalls = specialPermissionSet ? 2 : 1;
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realJiraAdapter.getAdapterApiUri()))
              .method(HttpMethod.DELETE),
          times(jiraDeleteCalls));

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
                    .with(httpBasic(testAdminUsername, VALID_CREDENTIAL)))
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
    ExecutionJob actualJob = resultProject.lastExecutionJobs.iterator().next();
    assertActualJobMatchesInputParams(actualJob, configuredResponse);

    // verify 2 repos are created
    assertEquals("Repository created", 2, resultProject.repositories.size());
  }

  private void assertActualJobMatchesInputParams(
      ExecutionJob actualJob, CreateProjectResponse configuredResponse) {
    String namespace = configuredResponse.extractNamespace();
    Assertions.assertThat(actualJob.getName())
        .isEqualTo(namespace + "-" + configuredResponse.extractBuildConfigName());

    Assertions.assertThat(actualJob.getUrl())
        .contains(
            format(
                "https://jenkins-%s.192.168.56.101.nip.io/job/%s/job/%s-%s/%s",
                namespace,
                namespace,
                namespace,
                configuredResponse.extractBuildConfigName(),
                configuredResponse.extractBuildNumber()));

    String expectedUrlSuffix =
        configuredResponse.extractBuildConfigName() + "/" + configuredResponse.extractBuildNumber();
    Assertions.assertThat(actualJob.getUrl()).endsWith(expectedUrlSuffix);
  }

  /** Test positive new quickstarter */
  @Test
  public void testQuickstarterProvisionOnNewOpenProject() throws Exception {
    testQuickstarterProvisionOnNewOpenProject(false);
  }

  /** Test positive new quickstarter and delete whole project afterwards */
  @Test
  public void testQuickstarterProvisionOnNewOpenProjectInclDeleteWholeProject() throws Exception {
    OpenProjectData createdProjectIncludingQuickstarters =
        testQuickstarterProvisionOnNewOpenProject(false);

    assertNotNull(createdProjectIncludingQuickstarters);
    assertNotNull(createdProjectIncludingQuickstarters.projectKey);
    assertNotNull(createdProjectIncludingQuickstarters.quickstarters);
    assertEquals(1, createdProjectIncludingQuickstarters.quickstarters.size());

    OpenProjectData toClean = new OpenProjectData();
    toClean.projectKey = createdProjectIncludingQuickstarters.projectKey;
    toClean.quickstarters = createdProjectIncludingQuickstarters.quickstarters;

    mockExecuteAdminJob("prov", "delete-projects", "testp");
    // delete whole projects (-cd, -dev and -test), calls
    // org.opendevstack.provision.controller.ProjectApiController.deleteProject

    mockMvc
        .perform(
            delete("/api/v2/project/" + toClean.projectKey)
                .contentType(MediaType.APPLICATION_JSON)
                .with(httpBasic(testAdminUsername, VALID_CREDENTIAL))
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  /** Test positive new quickstarter and delete single component afterwards */
  @Test
  public void testQuickstarterProvisionOnNewOpenProjectInclDeleteSingleComponent()
      throws Exception {
    OpenProjectData createdProjectIncludingQuickstarters =
        testQuickstarterProvisionOnNewOpenProject(false);

    assertNotNull(createdProjectIncludingQuickstarters);
    assertNotNull(createdProjectIncludingQuickstarters.projectKey);
    assertNotNull(createdProjectIncludingQuickstarters.quickstarters);
    assertEquals(1, createdProjectIncludingQuickstarters.quickstarters.size());

    OpenProjectData toClean = new OpenProjectData();
    toClean.projectKey = createdProjectIncludingQuickstarters.projectKey;
    toClean.quickstarters = createdProjectIncludingQuickstarters.quickstarters;

    String prefix = createdProjectIncludingQuickstarters.quickstarters.get(0).get("component_id");
    mockExecuteAdminJob("testp", "delete-components", prefix);

    // delete single component (via
    // org.opendevstack.provision.controller.ProjectApiController.deleteComponents)
    mockMvc
        .perform(
            delete("/api/v2/project/")
                .content(ProjectApiControllerTest.asJsonString(toClean))
                .contentType(MediaType.APPLICATION_JSON)
                .with(httpBasic(testAdminUsername, VALID_CREDENTIAL))
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  private void mockExecuteAdminJob(String namespace, String jobName, String prefix)
      throws IOException {
    CreateProjectResponse configuredResponse =
        CreateProjectResponseUtil.buildDummyCreateProjectResponse(namespace, "build-config", 1);

    String jenkinsJobPath =
        createJenkinsJobPath(namespace, jobName + "/Jenkinsfile", "ods-corejob-" + prefix);
    mockHelper
        .mockExecute(
            matchesClientCall()
                .url(containsString(jenkinsJobPath))
                .bodyMatches(instanceOf(Execution.class))
                .method(HttpMethod.POST))
        .thenReturn(configuredResponse);
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
    if (realLocalStorageAdapter.getProject(dataUpdate.projectKey) == null) {
      testProvisionNewSimpleProjectE2E();
    }

    OpenProjectData currentlyStoredProject =
        realLocalStorageAdapter.getProject(dataUpdate.projectKey);

    Assertions.assertThat(currentlyStoredProject.getQuickstarters()).isEmpty();

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

    CreateProjectResponse configuredCreateProjectResponse =
        CreateProjectResponseUtil.buildDummyCreateProjectResponse("demo-dev", "my-build-config", 2);

    OngoingStubbing<Object> stub =
        mockHelper.mockExecute(
            matchesClientCall()
                .url(
                    containsString(
                        "https://webhook-proxy-testp-cd.192.168.56.101.nip.io/build?trigger_secret="))
                .url(
                    containsString(
                        "&jenkinsfile_path=be-python-flask/Jenkinsfile&component=ods-qs-be-python"))
                .bodyMatches(instanceOf(Execution.class))
                .method(HttpMethod.POST));
    if (fail) {
      stub.thenThrow(
          new IOException("Provision via Jenkins fails, because this was requested in test."));
    } else {
      stub.thenReturn(configuredCreateProjectResponse);
    }

    // update the project with the new quickstarter
    MvcResult resultUpdateResponse =
        mockMvc
            .perform(
                put("/api/v2/project")
                    .content(ProjectApiControllerTest.asJsonString(dataUpdate))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(httpBasic(testAdminUsername, VALID_CREDENTIAL))
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

      // verify component based on repo in jira created
      mockHelper.verifyExecute(
          matchesClientCall()
              .url(containsString(realJiraAdapter.getAdapterApiUri() + "/component"))
              .method(HttpMethod.POST),
          times(1));

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
    ExecutionJob actualJob = resultProject.lastExecutionJobs.iterator().next();
    assertActualJobMatchesInputParams(actualJob, configuredCreateProjectResponse);

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

    copyTestFileToLocalStorageFolder(
        testProjectDataDir.toPath(),
        new File(realLocalStorageAdapter.getLocalStoragePath()).toPath(),
        "20190101171023-LEGPROJ.txt");

    MvcResult resultLegacyProjectGetResponse =
        mockMvc
            .perform(
                get("/api/v2/project/LEGPROJ")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(httpBasic(testAdminUsername, VALID_CREDENTIAL)))
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

  @Test
  public void getProjectQuickStarterDescription() throws Exception {

    copyTestFileToLocalStorageFolder(
        testProjectDataDir.toPath(),
        new File(realLocalStorageAdapter.getLocalStoragePath()).toPath(),
        "20190101171023-LEGPROJ.txt");

    mockMvc
        .perform(
            get("/api/v2/project/LEGPROJ")
                .with(httpBasic(testAdminUsername, VALID_CREDENTIAL))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            jsonPath(
                "$.quickstarters[0].component_type", is("9992a587-959c-4ceb-8e3f-c1390e40c582")))
        .andExpect(jsonPath("$.quickstarters[0].component_id", is("be-python-flask")))
        .andExpect(
            jsonPath("$.quickstarters[0].component_description", is("Backend - Python/Flask")))
        .andExpect(jsonPath("$.quickstarters[1].component_type", is("be-python-flask")))
        .andExpect(jsonPath("$.quickstarters[1].component_id", is("logviewer")))
        .andExpect(
            jsonPath("$.quickstarters[1].component_description", is("Backend - Python/Flask")))
        .andDo(MockMvcResultHandlers.print());
  }

  public static void copyTestFileToLocalStorageFolder(
      Path sourceDir, Path targetDir, String filename) throws IOException {
    Path source = new File(sourceDir.toFile(), filename).toPath();
    Path target = new File(targetDir.toFile(), filename).toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  /*
   * internal test helpers
   */
  public static <T> T readTestData(String name, Class<T> returnType) throws Exception {
    return new ObjectMapper().readValue(fileReader.findTestFile(name), returnType);
  }

  private <T> T readTestDataTypeRef(String name, TypeReference<T> returnType) throws Exception {
    return new ObjectMapper().readValue(fileReader.findTestFile(name), returnType);
  }

  private String createJenkinsJobPath(String namespace, String jenkinsfilePath, String component) {
    return "https://webhook-proxy-"
        + namespace
        + "-cd.192.168.56.101.nip.io/build?trigger_secret=secret101&jenkinsfile_path="
        + jenkinsfilePath
        + "&component="
        + component;
  }

  public static File createTempDir(File tempDir) {
    try {
      return Files.createTempDirectory(tempDir.toPath(), "e2e-test-temp-files").toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp directory in " + tempDir, e);
    }
  }
}
