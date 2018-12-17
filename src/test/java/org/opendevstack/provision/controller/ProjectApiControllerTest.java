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

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.ProjectIdentityMgmtAdapter;
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBoot.class)
@DirtiesContext
public class ProjectApiControllerTest {

  @Mock
  private JiraAdapter jiraAdapter;
  @Mock
  private ConfluenceAdapter confluenceAdapter;
  @Mock
  private BitbucketAdapter bitbucketAdapter;
  @Mock
  private RundeckAdapter rundeckAdapter;
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

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    initProjectData();
  }

  private void initProjectData() {
    data = new ProjectData();
    data.key = "KEY";
    data.name = "Name";
    data.description = "Description";

    Map<String, String> someQuickstarter = new HashMap<>();
    someQuickstarter.put("key", "value");
    List<Map<String,String>> quickstart = new ArrayList<>();
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
  public void addProjectWithoutOCPosNeg() throws Exception {
    Mockito.when(jiraAdapter.createJiraProjectForProject(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(confluenceAdapter.createConfluenceSpaceForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");
    Mockito.doNothing().when(client).removeClient(Matchers.anyString());
    
    Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);
    
    mockMvc.perform(post("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
    
    // rundeck should NOT have been called and neither bitbucket
    Mockito.verify(rundeckAdapter, Mockito.never()).createOpenshiftProjects(
    	Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    Mockito.verify(bitbucketAdapter, Mockito.never()).createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    
    // try with failing storage
    Mockito.when(storage.storeProject(data)).thenThrow(IOException.class);
    mockMvc.perform(post("/api/v1/project")
        .content(asJsonString(data))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError());
  }

  @Test
  public void addProjectWithOC() throws Exception {
	data.openshiftproject = true;	  
    Mockito.when(jiraAdapter.createJiraProjectForProject(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(confluenceAdapter.createConfluenceSpaceForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(bitbucketAdapter.createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(bitbucketAdapter.createRepositoriesForProject(
            Matchers.isNotNull(ProjectData.class),Matchers.isNull(String.class)))
        .thenReturn(data);
    Mockito.when(bitbucketAdapter.createAuxiliaryRepositoriesForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class),
        Matchers.isNotNull(String[].class))).thenReturn(data);
    Mockito.when(rundeckAdapter.createOpenshiftProjects(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class))).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");
    Mockito.doNothing().when(client).removeClient(Matchers.anyString());
    
    Mockito.doNothing().when(idm).validateIdSettingsOfProject(data);
    
    mockMvc.perform(post("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
    
    // rundeck should have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter, Mockito.times(1)).createOpenshiftProjects(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class));
    Mockito.verify(bitbucketAdapter, Mockito.times(1)).createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    Mockito.verify(bitbucketAdapter, Mockito.times(1)).createRepositoriesForProject(
            Matchers.isNotNull(ProjectData.class),Matchers.isNull(String.class));
  }
  
  
  @Test
  public void addProjectAndBadRequest() throws Exception {
	Mockito.doNothing().when(client).removeClient(Matchers.anyString());

    mockMvc
        .perform(post("/api/v1/project")
            .content("{}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andDo(MockMvcResultHandlers.print());

    
    
  }

  @Test
  public void addProjectAnd4xxClientResult() throws Exception {
    ProjectData data = new ProjectData();
    data.key = "KEY";
    data.name = "Name";

    mockMvc
        .perform(post("/api/v1/project")
            .content("")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateProjectWithProjectExists() throws Exception {
    Mockito.when(
        jiraAdapter.keyExists(Matchers.isNotNull(String.class), Matchers.isNull(String.class)))
        .thenReturn(true);

    mockMvc.perform(get("/api/v1/project/validate")
            .param("name", "project")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotAcceptable())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateProjectWithProjectNotExists() throws Exception {
    Mockito.when(
        jiraAdapter.keyExists(Matchers.isNotNull(String.class), Matchers.isNull(String.class)))
        .thenReturn(false);
   
    mockMvc.perform(get("/api/v1/project/validate")
            .param("name", "project")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateKeyWithKeyExists() throws Exception {
    Mockito
        .when(
            jiraAdapter.keyExists(Matchers.isNotNull(String.class), Matchers.isNull(String.class)))
        .thenReturn(true);
    mockMvc
        .perform(get("/api/v1/project/key/validate")
                .param("key", "project")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isNotAcceptable())
            .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void validateKeyWithKeyNotExists() throws Exception {
    Mockito.when(
            jiraAdapter.keyExists(Matchers.isNotNull(String.class), Matchers.isNull(String.class)))
        .thenReturn(false);
    
    mockMvc.perform(get("/api/v1/project/key/validate")
            .param("key", "project")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void generateKey() throws Exception {
    Mockito.when(jiraAdapter.buildProjectKey(Matchers.isNotNull(String.class))).thenReturn("PROJ");
    
    mockMvc
        .perform(get("/api/v1/project/key/generate")
                .param("name", "project")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void getProject () throws Exception
  {
    // arbitrary number
    mockMvc.perform(get("/api/v1/project/1")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andDo(MockMvcResultHandlers.print());
        
    Mockito.when(storage.getProject(Matchers.anyString())).thenReturn(data);
    
    mockMvc.perform(get("/api/v1/project/1")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());
  }
  
  @Test
  public void getProjects () throws Exception
  {
    mockMvc.perform(get("/api/v1/project/jira/all")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());

    mockMvc.perform(get("/api/v1/project/xxxx/all")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void updateProjectWithAndWithoutOC() throws Exception {
	data.openshiftproject = false;
    Mockito.when(jiraAdapter.createJiraProjectForProject(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(confluenceAdapter.createConfluenceSpaceForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(bitbucketAdapter.createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(data);
    Mockito.when(bitbucketAdapter.createRepositoriesForProject(
            Matchers.isNotNull(ProjectData.class),Matchers.isNull(String.class)))
        .thenReturn(data);
    Mockito.when(bitbucketAdapter.createAuxiliaryRepositoriesForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class),
        Matchers.isNotNull(String[].class))).thenReturn(data);
    Mockito.when(rundeckAdapter.createOpenshiftProjects(Matchers.isNotNull(ProjectData.class),
        Matchers.isNull(String.class))).thenReturn(data);
    Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(data);
    Mockito.when(storage.storeProject(data)).thenReturn("created");
    Mockito.doNothing().when(client).removeClient(Matchers.anyString());

    // not existing
    mockMvc.perform(put("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
    
    // existing - store prior
    Mockito.when(storage.getProject(Matchers.anyString())).thenReturn(data);

    // upgrade to OC
    ProjectData upgrade = copyFromProject(data);
    upgrade.openshiftproject = true;
    apiController.ocUpgradeAllowed = true;
    
    mockMvc.perform(put("/api/v1/project")
            .content(asJsonString(upgrade))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    // rundeck should have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter, Mockito.times(1)).createOpenshiftProjects(
    	Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    Mockito.verify(bitbucketAdapter, Mockito.times(1)).createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    
    // upgrade to OC with upgrade forbidden
    data.openshiftproject = false;
    Mockito.when(storage.getProject(Matchers.anyString())).thenReturn(data);
    upgrade.openshiftproject = true;
    
    apiController.ocUpgradeAllowed = false;
    mockMvc.perform(put("/api/v1/project")
            .content(asJsonString(upgrade))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
        .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString("upgrade")))
        .andDo(MockMvcResultHandlers.print());

    
    // now w/o upgrade
    upgrade.openshiftproject = false;
    
    mockMvc.perform(put("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    // rundeck should NOT have been called (and repo creation as well)
    Mockito.verify(rundeckAdapter).createOpenshiftProjects(
    	Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
    Mockito.verify(bitbucketAdapter).createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));

    // now w/o upgrade
    data.openshiftproject = true;
    upgrade.openshiftproject = false;
    
    mockMvc.perform(put("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());

    // rundeck should have been called (and repo creation as well)
    Mockito.verify(bitbucketAdapter, Mockito.times(2)).createBitbucketProjectsForProject(
        Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class));
  }
  
  @Test
  public void testProjectDescLengh () throws Exception 
  {
        data.description = 
          	"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890STOPHERE";

        ProjectData dataReturn = this.copyFromProject(data);
        apiController.shortenDescription(dataReturn); 
        
	    Mockito.when(jiraAdapter.createJiraProjectForProject(Matchers.isNotNull(ProjectData.class),
            Matchers.isNull(String.class))).thenReturn(dataReturn);
        Mockito.when(confluenceAdapter.createConfluenceSpaceForProject(
            Matchers.isNotNull(ProjectData.class), Matchers.isNull(String.class))).thenReturn(dataReturn);
        Mockito.doNothing().when(mailAdapter).notifyUsersAboutProject(dataReturn);
        Mockito.when(storage.storeProject(data)).thenReturn("created");
        Mockito.doNothing().when(client).removeClient(Matchers.anyString());
        
        Mockito.doNothing().when(idm).validateIdSettingsOfProject(dataReturn);
                
        mockMvc.perform(post("/api/v1/project")
                .content(asJsonString(data))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString(dataReturn.description + "\"")));
        
        // test with null
        apiController.shortenDescription(null);
        
        // test with content
        apiController.shortenDescription(data);
        assertEquals(dataReturn.description, data.description);
        
        // test with null description
        data.description = null;
        apiController.shortenDescription(data);        
  }
  
  private String asJsonString(final Object obj) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final String jsonContent = mapper.writeValueAsString(obj);
      return jsonContent;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ProjectData copyFromProject (ProjectData origin) {
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
