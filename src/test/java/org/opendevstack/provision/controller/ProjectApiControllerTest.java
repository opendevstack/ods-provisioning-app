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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.storage.IStorage;
import org.opendevstack.provision.util.RestClient;
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
  
  @InjectMocks
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
  }

  @Test
  public void addProject() throws Exception {
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
    
    mockMvc.perform(post("/api/v1/project")
            .content(asJsonString(data))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void addProjectAnd5xxResult() throws Exception {
	Mockito.doNothing().when(client).removeClient(Matchers.anyString());

    mockMvc
        .perform(post("/api/v1/project")
            .content("{}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is5xxServerError())
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

  private String asJsonString(final Object obj) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final String jsonContent = mapper.writeValueAsString(obj);
      return jsonContent;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
