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

package org.opendevstack.provision.services;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.model.jira.Shortcut;
import org.opendevstack.provision.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.jira.rest.client.domain.BasicUser;

/**
 * @author Brokmeier, Pascal
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class JiraAdapterTests {

  @Mock
  CustomAuthenticationManager manager;
  @Mock
  CrowdUserDetailsService service;
  
  List<FullJiraProject> projects = new ArrayList<>();

  @Mock
  RestClient client;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired
  @InjectMocks
  JiraAdapter jiraAdapter;

  @Before
  public void initTests() {
    MockitoAnnotations.initMocks(this);
    projects = new ArrayList<FullJiraProject>();
  }

  @Test
  public void createJiraProjectForProject() throws Exception {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);

    // delete in case it already exists
    String name = "TestProject";
    String crowdCookieValue = "xyz";

    CrowdUserDetails details = Mockito.mock(CrowdUserDetails.class);
    Authentication authentication = Mockito.mock(Authentication.class);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // get authentication mock
    Mockito.when(authentication.getPrincipal()).thenReturn(details);

    Mockito.when(service.loadUserByToken(crowdCookieValue)).thenReturn(details);
    
    Mockito.doNothing().when(client).getSessionId(null);
    
    Mockito.when(details.getUsername()).thenReturn("achmed");
    Mockito.when(details.getFullName()).thenReturn("achmed meyer");
    Mockito.doReturn(getReturnProject()).when(spyAdapter).callHttp(Matchers.anyString(),
        Matchers.anyString(), Matchers.anyString(), Matchers.any(FullJiraProject.class.getClass()), Matchers.anyBoolean(), Matchers.any(JiraAdapter.HTTP_VERB.class));

    
    ProjectData createdProject =
        spyAdapter.createJiraProjectForProject(getTestProject(name), crowdCookieValue);   
    
    assertEquals(getTestProject(name).key, createdProject.key);
    assertEquals(getTestProject(name).name, createdProject.name);
  }

  @Test
  public void callJiraCreateProjectApi() throws IOException {
    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    String crowdCookieValue = "value";
    FullJiraProject expectedProject = new FullJiraProject();

    Mockito.doReturn(expectedProject).when(spyAdapter).callHttp(Matchers.anyString(), Matchers.anyString(),
        Matchers.anyString(), Matchers.any(FullJiraProject.class.getClass()), Matchers.anyBoolean(), Matchers.any(JiraAdapter.HTTP_VERB.class));
    
    FullJiraProject createdProject = spyAdapter.callJiraCreateProjectApi(expectedProject, crowdCookieValue);

    assertEquals(expectedProject, createdProject);
  }

  @Test
  public void buildJiraProjectPojoFromApiProject() {
    ProjectData apiInput = getTestProject("TestProject");
    apiInput.key = "TestP";

    FullJiraProject fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    
    assertEquals(apiInput.name, fullJiraProject.getName());
    assertEquals(apiInput.key, fullJiraProject.getKey());
  }

  @Test
  public void buildProjectKey() {
    String shortName = "shrt";
    
    assertEquals("SHRT", jiraAdapter.buildProjectKey(shortName));
  }

  @Test
  public void projectExists() throws Exception
  {
    String projectNameTrue = "TESTP";
    String projectNameFalse = "TESTP_FALSE";

    ProjectData apiInput = getTestProject(projectNameTrue);
    apiInput.key = projectNameTrue;

    FullJiraProject fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    projects.add(fullJiraProject);

    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    Mockito.doNothing().when(client).getSessionId(null);
    Mockito.doReturn(projects).when(mocked).getProjects("CookieValue", projectNameTrue);

    assertTrue(mocked.keyExists(projectNameTrue, "CookieValue"));

    projects.clear();
    Mockito.doReturn(projects).when(mocked).getProjects("CookieValue", projectNameFalse);
    assertFalse(mocked.keyExists(projectNameFalse, "CookieValue"));
  }

  @Test
  public void testCreatePermissions () throws Exception 
  {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);
	Mockito.doNothing().when(client).getSessionId(null);

    String projectNameTrue = "TESTP";
    ProjectData apiInput = getTestProject(projectNameTrue);
    apiInput.key = projectNameTrue;

    apiInput.admin = "Clemens";
    apiInput.adminGroup = "AdminGroup";
    apiInput.userGroup = "UserGroup";
    apiInput.readonlyGroup = "ReadonlyGroup";
    
    PermissionScheme scheme = new PermissionScheme();
    scheme.setId("permScheme1");
    
    Mockito.doReturn(scheme).when(mocked).callHttp(Matchers.anyString(),
        Matchers.anyString(), Matchers.anyString(), 
        Matchers.any(PermissionScheme.class.getClass()), Matchers.anyBoolean(),
        Matchers.any(JiraAdapter.HTTP_VERB.class));
    
	int updates = mocked.createPermissions(apiInput, "crowdCookieValue");

    Mockito.verify(mocked, Mockito.times(1)).callHttp(Matchers.anyString(),
            Matchers.anyString(), Matchers.anyString(), 
            Matchers.eq(PermissionScheme.class), Matchers.anyBoolean(),
            Matchers.eq(JiraAdapter.HTTP_VERB.POST));

    Mockito.verify(mocked, Mockito.times(1)).callHttp(Matchers.anyString(),
            Matchers.anyString(), Matchers.anyString(), 
            Matchers.eq(FullJiraProject.class), Matchers.anyBoolean(),
            Matchers.eq(JiraAdapter.HTTP_VERB.PUT));

    assertEquals(1, updates);
  }

  @Test
  public void testCreateShortcuts () throws Exception 
  {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    ProjectData apiInput = getTestProject("testproject");
    
    Mockito.doReturn(Shortcut.class).when(mocked).callHttp(Matchers.anyString(),
        Matchers.anyString(), Matchers.anyString(), 
        Matchers.any(Shortcut.class.getClass()), Matchers.anyBoolean(),
        Matchers.any(JiraAdapter.HTTP_VERB.class));

    int shortcutsAdded = mocked.addShortcutsToProject(apiInput, "test");
    
    assertEquals(5, shortcutsAdded);
    
    Mockito.verify(mocked, Mockito.times(5)).callHttp(Matchers.anyString(),
        Matchers.anyString(), Matchers.anyString(), 
        Matchers.eq(Shortcut.class), Matchers.anyBoolean(),
        Matchers.eq(JiraAdapter.HTTP_VERB.POST));
    
    apiInput.jiraconfluencespace = false;
    mocked = Mockito.spy(jiraAdapter);
    shortcutsAdded = mocked.addShortcutsToProject(apiInput, "test");
    assertEquals(-1, shortcutsAdded);
    
    Mockito.verify(mocked, Mockito.never()).callHttp(Matchers.anyString(),
        Matchers.anyString(), Matchers.anyString(), 
        Matchers.eq(Shortcut.class), Matchers.anyBoolean(),
        Matchers.eq(JiraAdapter.HTTP_VERB.POST));
    
  }

  
  public static ProjectData getTestProject(String name) {
    ProjectData apiInput = new ProjectData();
    BasicUser admin = new BasicUser(null, "testuser", "test user");

    apiInput.admins = new ArrayList<BasicUser>();
    apiInput.admins.add(admin);
    apiInput.name = name;
    apiInput.description = "Test Description";
    apiInput.key = "TESTP";
    apiInput.admin = "Clemens";
    return apiInput;
  }

  private FullJiraProject getReturnProject() {
    return new FullJiraProject(URI.create("http://localhost"), "TESTP", null, null, null, null,
        null, null, null, null, null, null);
  }
}
