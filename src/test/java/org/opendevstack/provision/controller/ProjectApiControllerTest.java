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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.services.*;
import org.opendevstack.provision.storage.IStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Torsten Jaeschke
 * @author utschig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBoot.class)
@DirtiesContext
public class ProjectApiControllerTest {

  @Mock private IBugtrackerAdapter jiraAdapter;
  @Mock private ICollaborationAdapter confluenceAdapter;
  @Mock private ISCMAdapter bitbucketAdapter;
  @Mock private IJobExecutionAdapter jenkinsPipelineAdapter;
  @Mock private MailAdapter mailAdapter;
  @Mock private IStorage storage;
  @Mock private StorageAdapter filteredStorage;

  @Mock private CrowdProjectIdentityMgmtAdapter idm;

  @InjectMocks @Autowired private ProjectApiController apiController;

  private MockMvc mockMvc;

  private OpenProjectData data;

  @Value("${project.template.key.names}")
  private String projectKeys;

  @Autowired private JiraAdapter realJiraAdapter;

  @Autowired ConfluenceAdapter realConfluenceAdapter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    initOpenProjectData();
    when(jiraAdapter.isSpecialPermissionSchemeEnabled()).thenReturn(true);
  }

  private void initOpenProjectData() {
    data = new OpenProjectData();
    data.projectKey = "KEY";
    data.projectName = "Name";
    data.description = "Description";

    Map<String, String> someQuickstarter = new HashMap<>();
    someQuickstarter.put("key", "value");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(someQuickstarter);
    data.quickstarters = quickstarters;

    data.platformRuntime = false;
    data.specialPermissionSet = true;
    data.projectAdminUser = "clemens";
    data.projectAdminGroup = "group";
    data.projectUserGroup = "group";
    data.projectReadonlyGroup = "group";
  }

  @Test
  public void addProjectWithoutOCPosNeg() throws Exception {

    OpenProjectData bugTrackProject = copyFromProject(data);
    bugTrackProject.bugtrackerUrl = "bugtracker";

    String collaborationSpaceURL = "collspace";
    bugTrackProject.collaborationSpaceUrl = collaborationSpaceURL;

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
  public void addProjectWithOC() throws Exception {
    data.platformRuntime = true;
    data.quickstarters = null;

    OpenProjectData bugTrackProject = copyFromProject(data);
    String bugtrackerUrl = "bugtracker";
    bugTrackProject.bugtrackerUrl = bugtrackerUrl;
    String collaborationSpaceURL = "collspace";
    bugTrackProject.collaborationSpaceUrl = collaborationSpaceURL;

    when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(bugTrackProject);
    when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);

    OpenProjectData projectSCM = copyFromProject(bugTrackProject);

    projectSCM.scmvcsUrl = "scmspace";

    Map<String, Map<URL_TYPE, String>> repos = new HashMap<>();

    when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
        .thenReturn(projectSCM.scmvcsUrl);
    when(bitbucketAdapter.createComponentRepositoriesForODSProject(isNotNull())).thenReturn(repos);
    when(bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(isNotNull(), isNotNull()))
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

    verifyAddProjectAdapterCalls(times(1));
  }

  @Test
  public void whenOnyCheckPreconditionsThenDoNotCreateProject() throws Exception {
    data.platformRuntime = true;

    // Only check preconditions
    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .param(ProjectApiController.ADD_PROJECT_PARAM_NAME_ONLY_CHECK_PRECONDITIONS, "true")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(
                    "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"COMPLETED_SUCCESSFULLY\"}"))
        .andDo(MockMvcResultHandlers.print());

    verifyAddProjectAdapterCalls(times(0));
  }

  public void verifyAddProjectAdapterCalls(VerificationMode times)
      throws IOException, CreateProjectPreconditionException {
    Mockito.verify(jenkinsPipelineAdapter, times).createPlatformProjects(isNotNull());
    // check preconditions should be always called
    Mockito.verify(bitbucketAdapter, times(1)).checkCreateProjectPreconditions(isNotNull());
    Mockito.verify(bitbucketAdapter, times).createSCMProjectForODSProject(isNotNull());
    Mockito.verify(bitbucketAdapter, times).createComponentRepositoriesForODSProject(isNotNull());
    // jira components
    Mockito.verify(jiraAdapter, times)
        .createComponentsForProjectRepositories(isNotNull(), isNotNull());
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

    data.platformRuntime = true;

    List<String> preconditionFailures = new ArrayList<>();
    preconditionFailures.add("failure1");
    preconditionFailures.add("failure2");

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
                + String.join(CheckPreconditionsResponse.ERRORS_DELIMITER, preconditionFailures))
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
                    "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"FAILED\",\"errors\":[\"failure1\",\"failure2\"]}"));

    verifyAddProjectAdapterCalls(times(0));

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void whenCheckPreconditionsThrowsExceptionThenReturnServerError() throws Exception {

    data.platformRuntime = true;

    RuntimeException thrownInTest = new RuntimeException("thrown in unit test");
    when(bitbucketAdapter.checkCreateProjectPreconditions(isNotNull()))
        .thenThrow(
            new CreateProjectPreconditionException("bitbucket", data.projectKey, thrownInTest));

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
                .string(
                    "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"FAILED\",\"errors\":[\"class java.lang.RuntimeException was thrown in adapter 'bitbucket' while executing check preconditions for project 'KEY'. [message=thrown in unit test]\"]}"));

    verifyAddProjectAdapterCalls(times(0));

    // ensure that cleanup was NOT called
    Mockito.verify(jiraAdapter, times(0)).cleanup(isNotNull(), isNotNull());
  }

  @Test
  public void addProjectAgainstExistingOne() throws Exception {

    when(storage.getProject(data.projectKey)).thenReturn(data);

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
                            + data.projectKey
                            + ") or name "
                            + "("
                            + data.projectName
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
    data.projectKey = "KEY";
    data.platformRuntime = true;

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

    projects.put(data.projectKey, data);

    OpenProjectData copy = copyFromProject(data);
    copy.projectKey = copy.projectKey + 2;
    projects.put(copy.projectKey, copy);

    when(filteredStorage.getProjects()).thenReturn(projects);

    ResultActions resultActions =
        mockMvc
            .perform(get("/api/v2/project").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());

    String content = resultActions.andReturn().getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree(content);

    assertEquals(2, jsonNode.size());

    JsonNode node = jsonNode.findValue(copy.projectKey);

    assertEquals(copy.projectKey, node.get("projectKey").asText());
  }

  @Test
  public void givenGetAllProjects_whenException_thenInternalServerErrorResponse() throws Exception {

    when(filteredStorage.getProjects()).thenReturn(null);

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

    data.projectKey = "1";

    when(filteredStorage.getFilteredSingleProject("1")).thenReturn(data);

    mockMvc
        .perform(get("/api/v2/project/1").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void getProjectTemplateKeys() throws Exception {
    // list of keys - that we need to jsonify
    projectKeys = projectKeys.replace(",", "\",\"");
    mockMvc
        .perform(get("/api/v2/project/templates").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(CoreMatchers.containsString("[\"" + projectKeys + "\"]")))
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void getProjectTypeTemplatesForKey() throws Exception {
    apiController.jiraAdapter = realJiraAdapter;
    apiController.confluenceAdapter = realConfluenceAdapter;

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
    data.platformRuntime = false;
    data.quickstarters = null;

    OpenProjectData bugTrackProject = copyFromProject(data);
    String collaborationSpaceURL = "collspace";
    bugTrackProject.collaborationSpaceUrl = collaborationSpaceURL;

    Map<String, Map<URL_TYPE, String>> repos = new HashMap<>();

    when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(data);
    when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);

    OpenProjectData projectSCM = copyFromProject(data);

    projectSCM.scmvcsUrl = "scmspace";

    when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
        .thenReturn(projectSCM.scmvcsUrl);
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
    upgrade.projectKey = data.projectKey;
    upgrade.platformRuntime = true;
    apiController.ocUpgradeAllowed = true;

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
    data.platformRuntime = false;
    when(storage.getProject(anyString())).thenReturn(data);
    upgrade.platformRuntime = true;

    apiController.ocUpgradeAllowed = false;
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
    upgrade.platformRuntime = false;

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
    data.platformRuntime = true;
    upgrade.platformRuntime = false;

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
    apiController.ocUpgradeAllowed = true;

    data.platformRuntime = false;
    data.quickstarters = null;

    // existing - store prior
    when(storage.getProject(anyString())).thenReturn(data);

    // upgrade to OC - based on a quickstarter
    OpenProjectData upgrade = new OpenProjectData();
    upgrade.projectKey = data.projectKey;
    upgrade.platformRuntime = false;

    Map<String, String> newQS = new HashMap<>();
    newQS.put("component_type", "someComponentType");
    newQS.put("component_id", "someComponentName");

    upgrade.quickstarters = new ArrayList<>();
    upgrade.quickstarters.add(newQS);

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
  public void updateProjectWithValidAndInvalidComponentId() throws Exception {

    // allow upgrade
    apiController.ocUpgradeAllowed = true;

    data.platformRuntime = false;
    data.quickstarters = null;

    // existing - store prior
    when(storage.getProject(anyString())).thenReturn(data);

    // upgrade to OC - based on a quickstarter
    OpenProjectData upgrade = new OpenProjectData();
    upgrade.projectKey = data.projectKey;
    upgrade.platformRuntime = false;

    BiConsumer<String, Boolean> request =
        (componentId, successful) -> {
          Map<String, String> newQS = new HashMap<>();
          newQS.put("component_type", "someComponentType");
          newQS.put("component_id", componentId);

          upgrade.quickstarters = new ArrayList<>();
          upgrade.quickstarters.add(newQS);

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
    Arrays.asList(".-adfasfdasdfasdfsad", tooShort, tooLong, "", null).stream()
        .forEach(s -> request.accept(s, false));
  }

  @Test
  public void testProjectDescLengh() throws Exception {
    data.description =
        "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890STOPHERE";

    OpenProjectData dataReturn = this.copyFromProject(data);
    apiController.shortenDescription(dataReturn);

    dataReturn.bugtrackerUrl = "bugtracker";

    String collaborationSpaceURL = "collspace";
    dataReturn.collaborationSpaceUrl = collaborationSpaceURL;

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
                .string(CoreMatchers.containsString(dataReturn.description + "\"")));

    // test with null
    apiController.shortenDescription(null);

    // test with content
    apiController.shortenDescription(data);
    assertEquals(dataReturn.description, data.description);

    // test with null description
    data.description = null;
    apiController.shortenDescription(data);
  }

  public static String asJsonString(final Object obj) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final String jsonContent = mapper.writeValueAsString(obj);
      return jsonContent;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    data.projectKey = origin.projectKey;
    data.projectName = origin.projectName;
    data.description = origin.description;
    data.platformRuntime = origin.platformRuntime;
    data.scmvcsUrl = origin.scmvcsUrl;
    data.quickstarters = origin.quickstarters;
    data.specialPermissionSet = origin.specialPermissionSet;
    data.projectAdminUser = origin.projectAdminUser;
    data.projectAdminGroup = origin.projectAdminGroup;
    data.projectUserGroup = origin.projectUserGroup;
    data.projectReadonlyGroup = origin.projectReadonlyGroup;
    return data;
  }

  @Test
  public void validateQuickstartersAcceptNoNullOrEmptyParams() {

    // case parameter is null
    try {
      ProjectApiController.validateQuickstarters(
          null, new ArrayList<Consumer<Map<String, String>>>());
    } catch (IllegalArgumentException e) {
      // expected!
      Assert.assertTrue(e.getMessage().contains("null"));
    }

    // case validators list is empty
    try {
      ProjectApiController.validateQuickstarters(
          data, new ArrayList<Consumer<Map<String, String>>>());
    } catch (IllegalArgumentException e) {
      // expected!
      Assert.assertTrue(e.getMessage().contains("validators"));
    }

    // case data is not null and validators is not empty
    Consumer<Map<String, String>> acceptAllValidator =
        stringStringMap -> {
          return;
        };
    ProjectApiController.validateQuickstarters(data, Arrays.asList(acceptAllValidator));
  }

  @Test
  public void testFormatError() {

    String error = "error";

    Assert.assertEquals(
        error,
        ProjectApiController.formatError(
            null, CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS, error));

    String expectedJson =
        "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"FAILED\",\"errors\":[\"error\"]}";

    Assert.assertEquals(
        expectedJson,
        ProjectApiController.formatError(
            MediaType.APPLICATION_JSON_VALUE,
            CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS,
            error));
  }
}
