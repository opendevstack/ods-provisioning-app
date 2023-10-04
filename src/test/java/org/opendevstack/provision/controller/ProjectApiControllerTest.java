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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;
import static org.opendevstack.provision.config.AuthSecurityTestConfig.TEST_ADMIN_USERNAME;
import static org.opendevstack.provision.config.AuthSecurityTestConfig.TEST_VALID_CREDENTIAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.services.CrowdProjectIdentityMgmtAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.StorageAdapter;
import org.opendevstack.provision.services.openshift.OpenshiftClient;
import org.opendevstack.provision.storage.IStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"crowd", "utestcrowd", "quickstarters"})
@DirtiesContext
public class ProjectApiControllerTest {

  @MockBean private OpenshiftClient openshiftClient;

  @MockBean private IBugtrackerAdapter jiraAdapter;

  @MockBean private ICollaborationAdapter confluenceAdapter;

  @MockBean private ISCMAdapter bitbucketAdapter;

  @MockBean private IJobExecutionAdapter jenkinsPipelineAdapter;

  @MockBean private MailAdapter mailAdapter;

  @MockBean private IStorage storage;

  @MockBean private StorageAdapter storageAdapter;

  @MockBean private CrowdProjectIdentityMgmtAdapter idm;

  @Autowired private ProjectApiController apiController;

  @Autowired private List<String> projectTemplateKeyNames;

  @Autowired private MockMvc mockMvc;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String roleAdmin;

  private OpenProjectData data;

  public static String asJsonString(final Object obj) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  public void setUp() {
    GrantedAuthority auth = () -> roleAdmin;
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestAuthentication(TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL, List.of(auth)));

    initOpenProjectData();

    when(jiraAdapter.isSpecialPermissionSchemeEnabled()).thenReturn(true);

    when(openshiftClient.projects()).thenReturn(Set.of("default", "ods"));

    apiController.setCheckPreconditionsEnabled(true);

    // Reset status of api controller for each test
    apiController.setConfluenceAdapterEnable(true);
  }

  private void initOpenProjectData() {
    data = new OpenProjectData();
    data.setProjectKey("KEY");
    data.setProjectName("Name");
    data.setDescription("Description");

    Map<String, String> someQuickstarter = new HashMap<>();
    someQuickstarter.put("key", "value");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(someQuickstarter);
    data.setQuickstarters(quickstarters);

    data.setPlatformRuntime(false);
    data.setSpecialPermissionSet(true);
    data.setProjectAdminUser("clemens");
    data.setProjectAdminGroup("group");
    data.setProjectUserGroup("group");
    data.setProjectReadonlyGroup("group");
  }

  @Test
  public void addProjectWithoutOCPosNeg() throws Exception {

    OpenProjectData bugTrackProject = copyFromProject(data);
    bugTrackProject.setBugtrackerUrl("bugtracker");

    String collaborationSpaceURL = "collspace";
    bugTrackProject.setCollaborationSpaceUrl(collaborationSpaceURL);

    when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(bugTrackProject);
    when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    when(storage.storeProject(data)).thenReturn("created");

    Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    Mockito.verify(jenkinsPipelineAdapter, never()).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, never()).checkCreateProjectPreconditions(isNotNull());
    Mockito.verify(bitbucketAdapter, never()).createSCMProjectForODSProject(isNotNull());

    // try with failing storage
    when(storage.storeProject(data)).thenThrow(IOException.class);
    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError());
  }

  @Test
  public void addProjectWithOC() {
    List.of(true, false)
        .forEach(
            confluenceAdapterEnabled -> {
              try {
                apiController.setConfluenceAdapterEnable(confluenceAdapterEnabled);

                data.setPlatformRuntime(true);
                data.setQuickstarters(null);

                OpenProjectData bugTrackProject = copyFromProject(data);
                bugTrackProject.setBugtrackerUrl("bugtracker");
                String collaborationSpaceURL = "collspace";
                bugTrackProject.setCollaborationSpaceUrl(collaborationSpaceURL);

                when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull()))
                    .thenReturn(bugTrackProject);
                when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
                    .thenReturn(collaborationSpaceURL);

                OpenProjectData projectSCM = copyFromProject(bugTrackProject);

                projectSCM.setScmvcsUrl("scmspace");

                Map<String, Map<URL_TYPE, String>> repos = new HashMap<>();

                when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
                    .thenReturn(projectSCM.getScmvcsUrl());
                when(bitbucketAdapter.createComponentRepositoriesForODSProject(isNotNull()))
                    .thenReturn(repos);
                when(bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(
                        isNotNull(), isNotNull()))
                    .thenReturn(repos);
                when(jenkinsPipelineAdapter.createPlatformProjects(isNotNull())).thenReturn(data);
                Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
                when(storage.storeProject(data)).thenReturn("created");

                Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);

                mockMvc
                    .perform(
                        post("/api/v2/project")
                            .content(asJsonString(data))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andDo(MockMvcResultHandlers.print());

                verifyAddProjectAdapterCalls(times(1), times(confluenceAdapterEnabled ? 1 : 0));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  public void whenOnyCheckPreconditionsThenDoNotCreateProject() throws Exception {

    final AtomicBoolean initial = new AtomicBoolean(Boolean.TRUE);

    List.of(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE)
        .forEach(
            value -> {

              // Only check preconditions
              try {
                OpenProjectData projectData = copyFromProject(data);

                // First run platformRuntime and bugtrackerSpace are equal true
                // 2 and 3 run, either one is true the other is false
                projectData.setPlatformRuntime(initial.get() || value);
                projectData.setBugtrackerSpace(!projectData.isPlatformRuntime() || value);
                initial.set(projectData.isBugtrackerSpace());

                mockMvc
                    .perform(
                        post("/api/v2/project")
                            .content(asJsonString(projectData))
                            .param(
                                ProjectApiController
                                    .ADD_PROJECT_PARAM_NAME_ONLY_CHECK_PRECONDITIONS,
                                "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(
                        MockMvcResultMatchers.content()
                            .string(
                                "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"COMPLETED_SUCCESSFULLY\"}"))
                    .andDo(MockMvcResultHandlers.print());

              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    // each adapter should be called 2 times
    verifyCheckPreconditionsCalls(times(2), times(2));
  }

  public void verifyCheckPreconditionsCalls(VerificationMode platform, VerificationMode bugtracker)
      throws CreateProjectPreconditionException, IOException {
    Mockito.verify(bitbucketAdapter, platform).checkCreateProjectPreconditions(isNotNull());
    // jira components
    Mockito.verify(jiraAdapter, bugtracker).checkCreateProjectPreconditions(isNotNull());
    Mockito.verify(confluenceAdapter, bugtracker).checkCreateProjectPreconditions(isNotNull());

    Mockito.verify(jenkinsPipelineAdapter, never()).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, never()).createSCMProjectForODSProject(isNotNull());
  }

  public void verifyAddProjectAdapterCalls(VerificationMode times)
      throws IOException, CreateProjectPreconditionException {
    verifyAddProjectAdapterCalls(times, times);
  }

  public void verifyAddProjectAdapterCalls(VerificationMode times, VerificationMode confluenceTimes)
      throws IOException, CreateProjectPreconditionException {
    Mockito.verify(jenkinsPipelineAdapter, times).createPlatformProjects(isNotNull());
    // check preconditions should be always called
    Mockito.verify(bitbucketAdapter, times(1)).checkCreateProjectPreconditions(isNotNull());
    Mockito.verify(bitbucketAdapter, times).createSCMProjectForODSProject(isNotNull());
    Mockito.verify(bitbucketAdapter, times).createComponentRepositoriesForODSProject(isNotNull());
    // jira components
    Mockito.verify(jiraAdapter, times)
        .createComponentsForProjectRepositories(isNotNull(), isNotNull());
    // confluence
    Mockito.verify(confluenceAdapter, confluenceTimes)
        .createCollaborationSpaceForODSProject(isNotNull());

    Mockito.clearInvocations(
        jiraAdapter, confluenceAdapter, bitbucketAdapter, jenkinsPipelineAdapter);
  }

  @Test
  public void whenCheckPreconditionsBadParamThenBadRequest() throws Exception {

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .param(
                    ProjectApiController.ADD_PROJECT_PARAM_NAME_ONLY_CHECK_PRECONDITIONS,
                    "wrong-value")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andDo(MockMvcResultHandlers.print());

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void whenCheckPreconditionsFailThenReturnErrorInResponseBody() throws Exception {

    data.setPlatformRuntime(true);

    List<CheckPreconditionFailure> preconditionFailures = new ArrayList<>();
    preconditionFailures.add(CheckPreconditionFailure.getUnexistantUserInstance("failure1"));
    preconditionFailures.add(CheckPreconditionFailure.getUnexistantUserInstance("failure2"));

    StringBuffer expectedBody = new StringBuffer();
    expectedBody
        .append(
            CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS
                + CheckPreconditionsResponse.KEY_VALUE_SEPARATOR
                + CheckPreconditionsResponse.JobStatus.FAILED)
        .append(System.lineSeparator());
    expectedBody
        .append(
            CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS
                + CheckPreconditionsResponse.KEY_VALUE_SEPARATOR
                + String.join(
                    CheckPreconditionsResponse.ERRORS_DELIMITER,
                    Arrays.toString(preconditionFailures.toArray())))
        .append(System.lineSeparator());

    when(bitbucketAdapter.checkCreateProjectPreconditions(isNotNull()))
        .thenReturn(preconditionFailures);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"FAILED\",\"errors\":[{\"error-code\":\"UNEXISTANT_USER\",\"error-message\":\"failure1\"},{\"error-code\":\"UNEXISTANT_USER\",\"error-message\":\"failure2\"}]}"));

    verifyAddProjectAdapterCalls(times(0));

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void whenCheckPreconditionsThrowsExceptionThenReturnServerError() throws Exception {

    data.setPlatformRuntime(true);

    String errorMessage = "thrown in unit test";
    AdapterException thrownInTest = new AdapterException(new RuntimeException(errorMessage));
    when(bitbucketAdapter.checkCreateProjectPreconditions(isNotNull()))
        .thenThrow(
            new CreateProjectPreconditionException(
                "bitbucket", data.getProjectKey(), thrownInTest));

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andExpect(
            MockMvcResultMatchers.header()
                .string(
                    ProjectApiController.RETRY_AFTER_HEADER,
                    ProjectApiController.RETRY_AFTER_INTERVAL_IN_SECONDS))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(CoreMatchers.containsString("\"error-code\":\"EXCEPTION\"")));

    verifyAddProjectAdapterCalls(times(0));

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void addProjectAgainstExistingOne() throws Exception {

    when(storage.getProject(data.getProjectKey())).thenReturn(data);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
        .andDo(MockMvcResultHandlers.print())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    CoreMatchers.containsString(
                        "("
                            + data.getProjectKey()
                            + ") or name "
                            + "("
                            + data.getProjectName()
                            + ") already exists")));

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void addProjectEmptyAndBadRequest() throws Exception {

    mockMvc
        .perform(
            post("/api/v2/project")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void addProjectNullAnd4xxClientResult() throws Exception {
    mockMvc
        .perform(
            post("/api/v2/project")
                .content("")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void addProjectKeyOnlyAndExpectBadRequest() throws Exception {
    OpenProjectData data = new OpenProjectData();
    data.setProjectKey("KEY");
    data.setPlatformRuntime(true);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void addProjectInLegacyFormatErrorsOut() throws Exception {
    ProjectData oldLegacyData = new ProjectData();
    oldLegacyData.name = "abcName";
    oldLegacyData.key = "abcKey";

    // new endpoint - old format, fail!
    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(oldLegacyData))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

    // wrong (old) endpoint
    mockMvc
        .perform(
            post("/api/v1/project")
                .content(asJsonString(oldLegacyData))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void validateProjectWithProjectExists() throws Exception {
    when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(true);

    mockMvc
        .perform(
            get("/api/v2/project/validate")
                .param("projectName", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotAcceptable())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateProjectWithProjectNotExists() throws Exception {
    when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(false);

    mockMvc
        .perform(
            get("/api/v2/project/validate")
                .param("projectName", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateKeyWithKeyExists() throws Exception {
    when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(true);
    mockMvc
        .perform(
            get("/api/v2/project/key/validate")
                .param("projectKey", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotAcceptable())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateKeyWithKeyNotExists() throws Exception {
    when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(false);

    mockMvc
        .perform(
            get("/api/v2/project/key/validate")
                .param("projectKey", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void generateKey() throws Exception {
    when(jiraAdapter.buildProjectKey(isNotNull(String.class))).thenReturn("PROJ");

    mockMvc
        .perform(
            get("/api/v2/project/key/generate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void givenGetAllProjects_whenProjectFound_thenOK() throws Exception {

    Map<String, OpenProjectData> projects = new HashMap<>();

    projects.put(data.getProjectKey(), data);

    OpenProjectData copy = copyFromProject(data);
    copy.setProjectKey(copy.getProjectKey() + 2);
    projects.put(copy.getProjectKey(), copy);

    when(storageAdapter.getProjects()).thenReturn(projects);

    ResultActions resultActions =
        mockMvc
            .perform(get("/api/v2/project").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());

    String content = resultActions.andReturn().getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree(content);

    assertEquals(2, jsonNode.size());

    JsonNode node = jsonNode.findValue(copy.getProjectKey());

    assertEquals(copy.getProjectKey(), node.get("projectKey").asText());
  }

  @Test
  public void givenGetAllProjects_whenException_thenInternalServerErrorResponse() throws Exception {

    when(storageAdapter.getProjects()).thenReturn(null);

    mockMvc
        .perform(get("/api/v2/project").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
        .andExpect(MockMvcResultMatchers.content().string(""))
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void getProject() throws Exception {
    // arbitrary number
    mockMvc
        .perform(get("/api/v2/project/1").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andDo(MockMvcResultHandlers.print());

    data.setProjectKey("1");

    when(storageAdapter.getFilteredSingleProject("1")).thenReturn(data);

    mockMvc
        .perform(get("/api/v2/project/1").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void getProjectTemplateKeys() throws Exception {

    BiConsumer<MediaType, String> consumer =
        (mediaType, responseBody) -> {
          try {
            mockMvc
                .perform(get("/api/v2/project/templates").accept(mediaType))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                    MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(responseBody)))
                .andDo(MockMvcResultHandlers.print());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

    consumer.accept(
        MediaType.TEXT_PLAIN,
        ProjectTemplateType.createProjectTemplateKeysAsString(projectTemplateKeyNames));

    consumer.accept(
        MediaType.APPLICATION_JSON,
        ProjectApiController.createProjectTemplateKeysJson(projectTemplateKeyNames));

    consumer =
        (mediaType, responseBody) -> {
          try {
            mockMvc
                .perform(get("/api/v2/project/templates").accept(mediaType))
                .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
                .andExpect(
                    MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(responseBody)))
                .andDo(MockMvcResultHandlers.print());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

    ObjectMapper mapper = new ObjectMapper();
    String body =
        mapper.writeValueAsString(
            Map.of("ERROR:", "Unsupported accept type: " + MediaType.APPLICATION_OCTET_STREAM));

    consumer.accept(MediaType.APPLICATION_OCTET_STREAM, body);
  }

  @Test
  public void getProjectTypeTemplatesForKey() throws Exception {

    // setup mocks
    Map<IServiceAdapter.PROJECT_TEMPLATE, String> jiraTemplates = new HashMap<>();
    jiraTemplates.put(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_TYPE_KEY, "software");
    jiraTemplates.put(
        IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY,
        "com.pyxis.greenhopper.jira:gh-scrum-template");
    when(jiraAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(
            any(OpenProjectData.class)))
        .thenReturn(jiraTemplates);

    Map<IServiceAdapter.PROJECT_TEMPLATE, String> confluenceTemplates = new HashMap<>();
    confluenceTemplates.put(
        IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY,
        "com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint");
    when(confluenceAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(
            any(OpenProjectData.class)))
        .thenReturn(confluenceTemplates);

    // trigger requests
    mockMvc
        .perform(get("/api/v2/project/template/default").accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    CoreMatchers.containsString(
                        "bugTrackerTemplate\":\"software#com.pyxis.greenhopper.jira:gh-scrum-template\"")))
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    CoreMatchers.containsString(
                        "collabSpaceTemplate\":\"com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint")));

    mockMvc
        .perform(get("/api/v2/project/template/nonExistant").accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    CoreMatchers.containsString(
                        "bugTrackerTemplate\":\"software#com.pyxis.greenhopper.jira:gh-scrum-template\"")))
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    CoreMatchers.containsString(
                        "collabSpaceTemplate\":\"com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint")));

    mockMvc
        .perform(get("/api/v2/project/template/").accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().is4xxClientError());
  }

  @Test
  public void updateProjectWithAndWithoutOC() throws Exception {
    data.setPlatformRuntime(false);
    data.setQuickstarters(null);

    OpenProjectData bugTrackProject = copyFromProject(data);
    String collaborationSpaceURL = "collspace";
    bugTrackProject.setCollaborationSpaceUrl(collaborationSpaceURL);

    Map<String, Map<URL_TYPE, String>> repos = new HashMap<>();

    when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(data);
    when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);

    OpenProjectData projectSCM = copyFromProject(data);

    projectSCM.setScmvcsUrl("scmspace");

    when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
        .thenReturn(projectSCM.getScmvcsUrl());
    when(bitbucketAdapter.createComponentRepositoriesForODSProject(isNotNull())).thenReturn(repos);
    when(bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(isNotNull(), isNotNull()))
        .thenReturn(repos);
    when(jenkinsPipelineAdapter.createPlatformProjects(isNotNull())).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    when(storage.storeProject(data)).thenReturn("created");

    // not existing
    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound());

    // existing - store prior
    when(storage.getProject(anyString())).thenReturn(data);

    // upgrade to OC - with minimal set
    OpenProjectData upgrade = new OpenProjectData();
    upgrade.setProjectKey(data.getProjectKey());
    upgrade.setPlatformRuntime(true);
    apiController.setOcUpgradeAllowed(true);

    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(upgrade))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    Mockito.verify(jenkinsPipelineAdapter, times(1)).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, times(1)).createSCMProjectForODSProject(isNotNull());

    // upgrade to OC with upgrade forbidden
    data.setPlatformRuntime(false);
    when(storage.getProject(anyString())).thenReturn(data);
    upgrade.setPlatformRuntime(true);

    apiController.setOcUpgradeAllowed(false);
    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(upgrade))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
        .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString("upgrade")))
        .andDo(MockMvcResultHandlers.print());

    // now w/o upgrade
    upgrade.setPlatformRuntime(false);

    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    Mockito.verify(jenkinsPipelineAdapter).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter).createSCMProjectForODSProject(isNotNull());

    // now w/o upgrade
    data.setPlatformRuntime(true);
    upgrade.setPlatformRuntime(false);

    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    Mockito.verify(bitbucketAdapter, times(2)).createSCMProjectForODSProject(isNotNull());
  }

  @Test
  public void updateProjectWithQSAdditionOnly() throws Exception {
    // allow upgrade
    apiController.setOcUpgradeAllowed(true);

    data.setPlatformRuntime(false);
    data.setQuickstarters(null);

    // existing - store prior
    when(storage.getProject(anyString())).thenReturn(data);

    // upgrade to OC - based on a quickstarter
    OpenProjectData upgrade = new OpenProjectData();
    upgrade.setProjectKey(data.getProjectKey());
    upgrade.setPlatformRuntime(false);

    Map<String, String> newQS = new HashMap<>();
    newQS.put("component_type", "someComponentType");
    newQS.put("component_id", "someComponentName");

    upgrade.setQuickstarters(new ArrayList<>());
    upgrade.getQuickstarters().add(newQS);

    // this will error out - because of the test mock, but the key is scm project creation
    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(upgrade))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print());

    Mockito.verify(bitbucketAdapter).createSCMProjectForODSProject(isNotNull());
  }

  @Test
  public void updateProjectWithValidAndInvalidComponentId() {

    // allow upgrade
    apiController.setOcUpgradeAllowed(true);

    data.setPlatformRuntime(false);
    data.setQuickstarters(null);

    // existing - store prior
    when(storage.getProject(anyString())).thenReturn(data);

    // upgrade to OC - based on a quickstarter
    OpenProjectData upgrade = new OpenProjectData();
    upgrade.setProjectKey(data.getProjectKey());
    upgrade.setPlatformRuntime(false);

    BiConsumer<String, Boolean> request =
        (componentId, successful) -> {
          Map<String, String> newQS = new HashMap<>();
          newQS.put("component_type", "someComponentType");
          newQS.put("component_id", componentId);

          upgrade.setQuickstarters(new ArrayList<>());
          upgrade.getQuickstarters().add(newQS);

          // this will error out - because of the test mock, but the key is scm project creation
          try {
            mockMvc
                .perform(
                    put("/api/v2/project")
                        .content(asJsonString(upgrade))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    successful.equals(Boolean.FALSE)
                        ? MockMvcResultMatchers.status().isBadRequest()
                        : MockMvcResultMatchers.status().is2xxSuccessful());
          } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
          }
        };

    String tooShort = "ad";
    String tooLong = "1234567890123456789012345678901234567890addd";
    Arrays.asList(".-adfasfdasdfasdfsad", tooShort, tooLong, "", null)
        .forEach(s -> request.accept(s, false));
  }

  @Test
  public void testProjectDescLengh() throws Exception {
    data.setDescription(
        "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890STOPHERE");

    OpenProjectData dataReturn = this.copyFromProject(data);
    dataReturn.setDescription(ProjectApiController.createShortenedDescription(dataReturn));

    dataReturn.setBugtrackerUrl("bugtracker");

    String collaborationSpaceURL = "collspace";
    dataReturn.setCollaborationSpaceUrl(collaborationSpaceURL);

    when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(dataReturn);
    when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(dataReturn);
    when(storage.storeProject(data)).thenReturn("created");

    Mockito.doNothing().when(idm).validateIdSettingsOfProject(dataReturn);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(CoreMatchers.containsString(dataReturn.getDescription() + "\"")));

    // test with null
    try {
      ProjectApiController.createShortenedDescription(null);
      fail();
    } catch (IllegalArgumentException iae) {
      assertTrue(iae.getMessage().contains("project"));
    }

    // test with content
    String description = ProjectApiController.createShortenedDescription(data);
    assertEquals(dataReturn.getDescription(), description);

    // test with null description
    data.setDescription(null);
    description = ProjectApiController.createShortenedDescription(data);
    assertEquals(data.getDescription(), description);
  }

  @Test
  public void testProjectDeleteForbidden() {
    apiController.setCleanupAllowed(false);
    IOException testExForbidden = null;
    try {
      this.apiController.deleteProject("1245");
    } catch (IOException mustbeThrown) {
      testExForbidden = mustbeThrown;
      assertEquals("Cleanup of projects is NOT allowed", mustbeThrown.getMessage());
    }
    assertNotNull(testExForbidden);
  }

  @Test
  public void testProjectComponentDeleteForbidden() {
    IOException testExForbidden = null;
    apiController.setCleanupAllowed(false);
    try {
      this.apiController.deleteComponents(new OpenProjectData());
    } catch (IOException mustbeThrown) {
      testExForbidden = mustbeThrown;
      assertEquals("Cleanup of projects is NOT allowed", mustbeThrown.getMessage());
    }
    assertNotNull(testExForbidden);
  }

  private OpenProjectData copyFromProject(OpenProjectData origin) {
    OpenProjectData data = new OpenProjectData();
    data.setProjectKey(origin.getProjectKey());
    data.setProjectName(origin.getProjectName());
    data.setDescription(origin.getDescription());
    data.setPlatformRuntime(origin.isPlatformRuntime());
    data.setScmvcsUrl(origin.getScmvcsUrl());
    data.setQuickstarters(origin.getQuickstarters());

    data.setSpecialPermissionSet(origin.isSpecialPermissionSet());
    data.setProjectAdminUser(origin.getProjectAdminUser());
    data.setProjectAdminGroup(origin.getProjectAdminGroup());
    data.setProjectUserGroup(origin.getProjectUserGroup());
    data.setProjectReadonlyGroup(origin.getProjectReadonlyGroup());
    return data;
  }

  @Test
  public void validateQuickstartersAcceptNoNullOrEmptyParams() {

    // case parameter is null
    try {
      ProjectApiController.validateQuickstarters(null, new ArrayList<>());
    } catch (IllegalArgumentException e) {
      // expected!
      assertTrue(e.getMessage().contains("null"));
    }

    // case validators list is empty
    try {
      ProjectApiController.validateQuickstarters(data, new ArrayList<>());
    } catch (IllegalArgumentException e) {
      // expected!
      assertTrue(e.getMessage().contains("validators"));
    }

    // case data is not null and validators is not empty
    Consumer<Map<String, String>> acceptAllValidator = stringStringMap -> {};
    ProjectApiController.validateQuickstarters(data, Collections.singletonList(acceptAllValidator));
  }
}
