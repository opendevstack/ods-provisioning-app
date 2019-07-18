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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.CrowdProjectIdentityMgmtAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.StorageAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
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
  @Mock private IJobExecutionAdapter rundeckAdapter;
  @Mock private MailAdapter mailAdapter;
  @Mock private IStorage storage;
  @Mock private StorageAdapter filteredStorage;

  @Mock private RestClient client;

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

    Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull()))
        .thenReturn(bugTrackProject);
    Mockito.when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");

    Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    // rundeck should NOT have been called and neither bitbucket
    Mockito.verify(rundeckAdapter, Mockito.never()).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, Mockito.never()).createSCMProjectForODSProject(isNotNull());

    // try with failing storage
    Mockito.when(storage.storeProject(data)).thenThrow(IOException.class);
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

    Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull()))
        .thenReturn(bugTrackProject);
    Mockito.when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);

    OpenProjectData projectSCM = copyFromProject(bugTrackProject);

    projectSCM.scmvcsUrl = "scmspace";

    Map<String, Map<URL_TYPE, String>> repos = new HashMap<>();

    Mockito.when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
        .thenReturn(projectSCM.scmvcsUrl);
    Mockito.when(bitbucketAdapter.createComponentRepositoriesForODSProject(isNotNull()))
        .thenReturn(repos);
    Mockito.when(
            bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(isNotNull(), isNotNull()))
        .thenReturn(repos);
    Mockito.when(rundeckAdapter.createPlatformProjects(isNotNull())).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");

    Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    // rundeck should have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter, Mockito.times(1)).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, Mockito.times(1)).createSCMProjectForODSProject(isNotNull());
    Mockito.verify(bitbucketAdapter, Mockito.times(1))
        .createComponentRepositoriesForODSProject(isNotNull());
    // jira components
    Mockito.verify(jiraAdapter, Mockito.times(1))
        .createComponentsForProjectRepositories(isNotNull());
  }

  @Test
  public void addProjectAgainstExistingOne() throws Exception {

    Mockito.when(storage.getProject(data.projectKey)).thenReturn(data);

    mockMvc
        .perform(
            post("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
        .andExpect(
            MockMvcResultMatchers.content()
                .string(CoreMatchers.containsString(data.projectKey + ") already exists")))
        .andDo(MockMvcResultHandlers.print());
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
  public void validateProjectWithProjectExists() throws Exception {
    Mockito.when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(true);

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
    Mockito.when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(false);

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
    Mockito.when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(true);
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
    Mockito.when(jiraAdapter.projectKeyExists(isNotNull(String.class))).thenReturn(false);

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
    Mockito.when(jiraAdapter.buildProjectKey(isNotNull(String.class))).thenReturn("PROJ");

    mockMvc
        .perform(
            get("/api/v2/project/key/generate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
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

    Mockito.when(filteredStorage.getFilteredSingleProject("1")).thenReturn(data);

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

    Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull())).thenReturn(data);
    Mockito.when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);

    OpenProjectData projectSCM = copyFromProject(data);

    projectSCM.scmvcsUrl = "scmspace";

    Mockito.when(bitbucketAdapter.createSCMProjectForODSProject(isNotNull()))
        .thenReturn(projectSCM.scmvcsUrl);
    Mockito.when(bitbucketAdapter.createComponentRepositoriesForODSProject(isNotNull()))
        .thenReturn(repos);
    Mockito.when(
            bitbucketAdapter.createAuxiliaryRepositoriesForODSProject(isNotNull(), isNotNull()))
        .thenReturn(repos);
    Mockito.when(rundeckAdapter.createPlatformProjects(isNotNull())).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");

    // not existing
    mockMvc
        .perform(
            put("/api/v2/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound());

    // existing - store prior
    Mockito.when(storage.getProject(anyString())).thenReturn(data);

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

    // rundeck should have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter, Mockito.times(1)).createPlatformProjects(isNotNull());
    Mockito.verify(bitbucketAdapter, Mockito.times(1)).createSCMProjectForODSProject(isNotNull());

    // upgrade to OC with upgrade forbidden
    data.platformRuntime = false;
    Mockito.when(storage.getProject(anyString())).thenReturn(data);
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

    // rundeck should NOT have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter).createPlatformProjects(isNotNull());
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

    // rundeck should have been called (and repo creation as well)
    Mockito.verify(bitbucketAdapter, Mockito.times(2)).createSCMProjectForODSProject(isNotNull());
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

    Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(isNotNull()))
        .thenReturn(dataReturn);
    Mockito.when(confluenceAdapter.createCollaborationSpaceForODSProject(isNotNull()))
        .thenReturn(collaborationSpaceURL);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(dataReturn);
    Mockito.when(storage.storeProject(data)).thenReturn("created");

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
    apiController.cleanupAllowed = false;
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
    apiController.cleanupAllowed = false;
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
}
