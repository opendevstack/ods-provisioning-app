/*
 * Copyright 2018 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.services.*;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBoot.class)
@DirtiesContext
public class ProjectApiControllerTest
{

    @Mock
    private IBugtrackerAdapter jiraAdapter;
    @Mock
    private ICollaborationAdapter confluenceAdapter;
    @Mock
    private ISCMAdapter bitbucketAdapter;
    @Mock
    private IJobExecutionAdapter rundeckAdapter;
    @Mock
    private MailAdapter mailAdapter;
    @Mock
    private IStorage storage;

    @Mock
    private RestClient client;

    @Mock
    private ProjectIdentityMgmtAdapter idm;

    @InjectMocks
    @Autowired
    private ProjectApiController apiController;

    private MockMvc mockMvc;

    private ProjectData data;

    @Value("${project.template.key.names}")
    private String projectKeys;

    @Autowired
    private JiraAdapter realJiraAdapter;

    @Autowired
    ConfluenceAdapter realConfluenceAdapter;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(apiController)
                .build();
        initProjectData();
    }

    private void initProjectData()
    {
        data = new ProjectData();
        data.key = "KEY";
        data.name = "Name";
        data.description = "Description";

        Map<String, String> someQuickstarter = new HashMap<>();
        someQuickstarter.put("key", "value");
        List<Map<String, String>> quickstart = new ArrayList<>();
        quickstart.add(someQuickstarter);
        data.quickstart = quickstart;

        data.openshiftproject = false;
        data.createpermissionset = true;
        data.admin = "clemens";
        data.adminGroup = "group";
        data.userGroup = "group";
        data.readonlyGroup = "group";
    }

    @Test
    public void addProjectWithoutOCPosNeg() throws Exception
    {
        Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(
                isNotNull(), isNull())).thenReturn(data);
        Mockito.when(confluenceAdapter
                .createCollaborationSpaceForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(data);
        Mockito.doNothing().when(mailAdapter)
                .notifyUsersAboutProject(data);
        Mockito.when(storage.storeProject(data))
                .thenReturn("created");
        Mockito.doNothing().when(client).removeClient(anyString());

        Mockito.doNothing().when(idm)
                .validateIdSettingsOfProject(data);

        mockMvc.perform(
                post("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        // rundeck should NOT have been called and neither bitbucket
        Mockito.verify(rundeckAdapter, Mockito.never())
                .createOpenshiftProjects(isNotNull(), isNull());
        Mockito.verify(bitbucketAdapter, Mockito.never())
                .createSCMProjectForODSProject(isNotNull(), isNull());

        // try with failing storage
        Mockito.when(storage.storeProject(data))
                .thenThrow(IOException.class);
        mockMvc.perform(
                post("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .is5xxServerError());
    }

    @Test
    public void addProjectWithOC() throws Exception
    {
        data.openshiftproject = true;
        Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(
                isNotNull(), isNull())).thenReturn(data);
        Mockito.when(confluenceAdapter
                .createCollaborationSpaceForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createSCMProjectForODSProject(isNotNull(), isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createComponentRepositoriesForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createAuxiliaryRepositoriesForODSProject(isNotNull(),
                        isNull(), isNotNull()))
                .thenReturn(data);
        Mockito.when(rundeckAdapter
                .createOpenshiftProjects(isNotNull(), isNull()))
                .thenReturn(data);
        Mockito.doNothing().when(mailAdapter)
                .notifyUsersAboutProject(data);
        Mockito.when(storage.storeProject(data))
                .thenReturn("created");
        Mockito.doNothing().when(client).removeClient(anyString());

        Mockito.doNothing().when(idm)
                .validateIdSettingsOfProject(data);

        mockMvc.perform(
                post("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        // rundeck should have been called (and repo creation as well)
        Mockito.verify(rundeckAdapter, Mockito.times(1))
                .createOpenshiftProjects(isNotNull(), isNull());
        Mockito.verify(bitbucketAdapter, Mockito.times(1))
                .createSCMProjectForODSProject(isNotNull(), isNull());
        Mockito.verify(bitbucketAdapter, Mockito.times(1))
                .createComponentRepositoriesForODSProject(isNotNull(),
                        isNull());
    }

    @Test
    public void addProjectEmptyAndBadRequest() throws Exception
    {
        Mockito.doNothing().when(client).removeClient(anyString());

        mockMvc.perform(post("/api/v1/project").content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void addProjectNullAnd4xxClientResult() throws Exception
    {
        mockMvc.perform(post("/api/v1/project").content("")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .is4xxClientError())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void addProjectKeyOnlyAndExpectBadRequest()
            throws Exception
    {
        ProjectData data = new ProjectData();
        data.key = "KEY";
        data.openshiftproject = true;

        mockMvc.perform(
                post("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void validateProjectWithProjectExists() throws Exception
    {
        Mockito.when(jiraAdapter.keyExists(isNotNull(String.class),
                isNull())).thenReturn(true);

        mockMvc.perform(get("/api/v1/project/validate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isNotAcceptable())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void validateProjectWithProjectNotExists() throws Exception
    {
        Mockito.when(jiraAdapter.keyExists(isNotNull(String.class),
                isNull())).thenReturn(false);

        mockMvc.perform(get("/api/v1/project/validate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void validateKeyWithKeyExists() throws Exception
    {
        Mockito.when(jiraAdapter.keyExists(isNotNull(String.class),
                isNull())).thenReturn(true);
        mockMvc.perform(get("/api/v1/project/key/validate")
                .param("key", "project")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isNotAcceptable())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void validateKeyWithKeyNotExists() throws Exception
    {
        Mockito.when(jiraAdapter.keyExists(isNotNull(String.class),
                isNull())).thenReturn(false);

        mockMvc.perform(get("/api/v1/project/key/validate")
                .param("key", "project")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void generateKey() throws Exception
    {
        Mockito.when(
                jiraAdapter.buildProjectKey(isNotNull(String.class)))
                .thenReturn("PROJ");

        mockMvc.perform(get("/api/v1/project/key/generate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getProject() throws Exception
    {
        // arbitrary number
        mockMvc.perform(get("/api/v1/project/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print());

        Mockito.when(storage.getProject(anyString()))
                .thenReturn(data);

        mockMvc.perform(get("/api/v1/project/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getProjectTemplateKeys() throws Exception
    {
        // list of keys - that we need to jsonify
        projectKeys = projectKeys.replace(",", "\",\"");
        mockMvc.perform(get("/api/v1/project/templates")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                "[\"" + projectKeys + "\"]")))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void getProjectTypeTemplatesForKey() throws Exception
    {
        apiController.jiraAdapter = realJiraAdapter;
        apiController.confluenceAdapter = realConfluenceAdapter;

        mockMvc.perform(get("/api/v1/project/template/default")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                "bugTrackerTemplate\":\"software#com.pyxis.greenhopper.jira:gh-scrum-template\"")))
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                "collabSpaceTemplate\":\"com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint")));

        mockMvc.perform(get("/api/v1/project/template/nonExistant")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                "bugTrackerTemplate\":\"software#com.pyxis.greenhopper.jira:gh-scrum-template\"")))
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                "collabSpaceTemplate\":\"com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint")));

        mockMvc.perform(get("/api/v1/project/template/")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status()
                        .is4xxClientError());
    }

    @Test
    public void updateProjectWithAndWithoutOC() throws Exception
    {
        data.openshiftproject = false;
        Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(
                isNotNull(), isNull())).thenReturn(data);
        Mockito.when(confluenceAdapter
                .createCollaborationSpaceForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createSCMProjectForODSProject(isNotNull(), isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createComponentRepositoriesForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(data);
        Mockito.when(bitbucketAdapter
                .createAuxiliaryRepositoriesForODSProject(isNotNull(),
                        isNull(), isNotNull()))
                .thenReturn(data);
        Mockito.when(rundeckAdapter
                .createOpenshiftProjects(isNotNull(), isNull()))
                .thenReturn(data);
        Mockito.doNothing().when(mailAdapter)
                .notifyUsersAboutProject(data);
        Mockito.when(storage.storeProject(data))
                .thenReturn("created");
        Mockito.doNothing().when(client).removeClient(anyString());

        // not existing
        mockMvc.perform(
                put("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.status().isNotFound());

        // existing - store prior
        Mockito.when(storage.getProject(anyString()))
                .thenReturn(data);

        // upgrade to OC - with minimal set
        ProjectData upgrade = new ProjectData();
        upgrade.key = data.key;
        upgrade.openshiftproject = true;
        apiController.ocUpgradeAllowed = true;

        mockMvc.perform(
                put("/api/v1/project").content(asJsonString(upgrade))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        // rundeck should have been called (and repo creation as well)
        Mockito.verify(rundeckAdapter, Mockito.times(1))
                .createOpenshiftProjects(isNotNull(), isNull());
        Mockito.verify(bitbucketAdapter, Mockito.times(1))
                .createSCMProjectForODSProject(isNotNull(), isNull());

        // upgrade to OC with upgrade forbidden
        data.openshiftproject = false;
        Mockito.when(storage.getProject(anyString()))
                .thenReturn(data);
        upgrade.openshiftproject = true;

        apiController.ocUpgradeAllowed = false;
        mockMvc.perform(
                put("/api/v1/project").content(asJsonString(upgrade))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .is5xxServerError())
                .andExpect(MockMvcResultMatchers.content().string(
                        CoreMatchers.containsString("upgrade")))
                .andDo(MockMvcResultHandlers.print());

        // now w/o upgrade
        upgrade.openshiftproject = false;

        mockMvc.perform(
                put("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        // rundeck should NOT have been called (and repo creation as well)
        Mockito.verify(rundeckAdapter)
                .createOpenshiftProjects(isNotNull(), isNull());
        Mockito.verify(bitbucketAdapter)
                .createSCMProjectForODSProject(isNotNull(), isNull());

        // now w/o upgrade
        data.openshiftproject = true;
        upgrade.openshiftproject = false;

        mockMvc.perform(
                put("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        // rundeck should have been called (and repo creation as well)
        Mockito.verify(bitbucketAdapter, Mockito.times(2))
                .createSCMProjectForODSProject(isNotNull(), isNull());
    }

    @Test
    public void testProjectDescLengh() throws Exception
    {
        data.description = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890STOPHERE";

        ProjectData dataReturn = this.copyFromProject(data);
        apiController.shortenDescription(dataReturn);

        Mockito.when(jiraAdapter.createBugtrackerProjectForODSProject(
                isNotNull(), isNull())).thenReturn(dataReturn);
        Mockito.when(confluenceAdapter
                .createCollaborationSpaceForODSProject(isNotNull(),
                        isNull()))
                .thenReturn(dataReturn);
        Mockito.doNothing().when(mailAdapter)
                .notifyUsersAboutProject(dataReturn);
        Mockito.when(storage.storeProject(data))
                .thenReturn("created");
        Mockito.doNothing().when(client).removeClient(anyString());

        Mockito.doNothing().when(idm)
                .validateIdSettingsOfProject(dataReturn);

        mockMvc.perform(
                post("/api/v1/project").content(asJsonString(data))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.content()
                        .string(CoreMatchers.containsString(
                                dataReturn.description + "\"")));

        // test with null
        apiController.shortenDescription(null);

        // test with content
        apiController.shortenDescription(data);
        assertEquals(dataReturn.description, data.description);

        // test with null description
        data.description = null;
        apiController.shortenDescription(data);
    }

    private String asJsonString(final Object obj)
    {
        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonContent = mapper.writeValueAsString(obj);
            return jsonContent;
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private ProjectData copyFromProject(ProjectData origin)
    {
        ProjectData data = new ProjectData();
        data.key = origin.key;
        data.name = origin.name;
        data.description = origin.description;
        data.openshiftproject = origin.openshiftproject;
        data.bitbucketUrl = origin.bitbucketUrl;
        data.quickstart = origin.quickstart;
        data.createpermissionset = origin.createpermissionset;
        data.admin = origin.admin;
        data.adminGroup = origin.adminGroup;
        data.userGroup = origin.userGroup;
        data.readonlyGroup = origin.readonlyGroup;
        return data;
    }

}
