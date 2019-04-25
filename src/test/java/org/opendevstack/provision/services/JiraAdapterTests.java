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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
import org.opendevstack.provision.util.exception.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author Brokmeier, Pascal
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class JiraAdapterTests {

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
  
  @Autowired
  Environment env;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;  
  
  @Autowired
  List<String> projectTemplateKeyNames;
  
  @Before
  public void initTests() {
    MockitoAnnotations.initMocks(this);
    projects = new ArrayList<>();
  }

  @Test
  public void createJiraProjectForProject() throws Exception {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    
    assertEquals("10000", spyAdapter.jiraNotificationSchemeId);

    // delete in case it already exists
    String name = "TestProject";
    String crowdCookieValue = "xyz";

    CrowdUserDetails details = Mockito.mock(CrowdUserDetails.class);
    Authentication authentication = Mockito.mock(Authentication.class);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // get authentication mock
    when(authentication.getPrincipal()).thenReturn(details);

    when(service.loadUserByToken(crowdCookieValue)).thenReturn(details);
    
    Mockito.doNothing().when(client).getSessionId(null);
    
    when(details.getUsername()).thenReturn("achmed");
    when(details.getFullName()).thenReturn("achmed meyer");

    when(client.callHttp(anyString(), any(FullJiraProject.class), anyString(), anyBoolean(), eq(RestClient.HTTP_VERB.POST),
            eq(FullJiraProject.class))).thenReturn(getReturnProject());

    ProjectData createdProject =
        spyAdapter.createJiraProjectForProject(getTestProject(name), crowdCookieValue);   
    
    assertEquals(getTestProject(name).key, createdProject.key);
    assertEquals(getTestProject(name).name, createdProject.name);
    // default template
    assertEquals(defaultProjectKey, createdProject.projectType);
    
    // new template
    ProjectData templateProject = getTestProject(name);
    templateProject.projectType="newTemplate";
    
    projectTemplateKeyNames.add("newTemplate");
    spyAdapter.environment.getSystemProperties().put("jira.project.template.key.newTemplate", "templateKey");
    spyAdapter.environment.getSystemProperties().put("jira.project.template.type.newTemplate", "templateType");
    
    ProjectData createdProjectWithNewTemplate =
        spyAdapter.createJiraProjectForProject(templateProject, crowdCookieValue);   
    
    assertEquals(templateProject.key, createdProjectWithNewTemplate.key);
    assertEquals(templateProject.name, createdProjectWithNewTemplate.name);
    assertEquals("newTemplate", createdProjectWithNewTemplate.projectType);

    HttpException thrownEx = null;
    try 
    {
        HttpException ioEx = new HttpException(300, "testerror");
        
        when(client.callHttp(anyString(), any(FullJiraProject.class), anyString(), anyBoolean(), any(RestClient.HTTP_VERB.class),
              eq(FullJiraProject.class))).thenThrow(ioEx);

      spyAdapter.createJiraProjectForProject(getTestProject(name), crowdCookieValue);
    } catch (HttpException e) 
    {
    	thrownEx = e;
    }
    assertNotNull(thrownEx);
    assertEquals(300, thrownEx.getResponseCode());
  }

  @Test
  public void callJiraCreateProjectApi() throws IOException {
    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    String crowdCookieValue = "value";
    FullJiraProject expectedProject = new FullJiraProject();

    when(client.callHttp(anyString(), any(FullJiraProject.class), anyString(), anyBoolean(), eq(RestClient.HTTP_VERB.POST),
            eq(FullJiraProject.class))).thenReturn(expectedProject);

    FullJiraProject createdProject = spyAdapter.callJiraCreateProjectApi(expectedProject, crowdCookieValue);

    assertEquals(expectedProject, createdProject);
  }

  @Test
  public void buildJiraProjectPojoFromApiProject() throws Exception
  {
    ProjectData apiInput = getTestProject("TestProject");
    apiInput.key = "TestP";

    FullJiraProject fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    
    assertEquals(apiInput.name, fullJiraProject.getName());
    assertEquals(apiInput.key, fullJiraProject.getKey());
    assertEquals(env.getProperty("jira.project.template.key"), fullJiraProject.projectTemplateKey);
    assertEquals(env.getProperty("jira.project.template.type"), fullJiraProject.projectTypeKey);
    
    apiInput.projectType = "notFound";
    fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    assertEquals(env.getProperty("jira.project.template.key"), fullJiraProject.projectTemplateKey);
    assertEquals(env.getProperty("jira.project.template.type"), fullJiraProject.projectTypeKey);
    
    apiInput.projectType = "newTemplate";

    // set them adhoc
    projectTemplateKeyNames.add("newTemplate");
    
    jiraAdapter.environment.getSystemProperties().put("jira.project.template.key.newTemplate", "template");
    jiraAdapter.environment.getSystemProperties().put("jira.project.template.type.newTemplate", "templateType");
    fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    assertEquals("template", fullJiraProject.projectTemplateKey);
    assertEquals("templateType", fullJiraProject.projectTypeKey);
    
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    
    System.out.println(ow.writeValueAsString(fullJiraProject));
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
    
    when(client.callHttp(anyString(),
            any(),
            anyString(), anyBoolean(),
            eq(RestClient.HTTP_VERB.POST), eq(PermissionScheme.class))).
    	thenReturn(scheme);
    
	int updates = mocked.createPermissions(apiInput, "crowdCookieValue");

    Mockito.verify(client, Mockito.times(1)).callHttp(anyString(),
            any(),
            anyString(), anyBoolean(),
            eq(RestClient.HTTP_VERB.POST), eq(PermissionScheme.class));

    Mockito.verify(client, Mockito.times(1)).callHttp(anyString(),
            any(),
            anyString(), anyBoolean(),
            eq(RestClient.HTTP_VERB.PUT), eq(FullJiraProject.class));
    
    assertEquals(1, updates);
  }

  @Test
  public void testCreateShortcuts () throws Exception 
  {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    RestClient restClientMocket = Mockito.spy(RestClient.class);
    ProjectData apiInput = getTestProject("testproject");
    
    int shortcutsAdded = mocked.addShortcutsToProject(apiInput, "test");
    
    assertEquals(5, shortcutsAdded);
    
    Mockito.verify(client, Mockito.times(5)).callHttp(anyString(),
        any(),
        anyString(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), eq(Shortcut.class));
    
    apiInput.jiraconfluencespace = false;
    mocked = Mockito.spy(jiraAdapter);
    mocked.client = Mockito.spy(RestClient.class);
    shortcutsAdded = mocked.addShortcutsToProject(apiInput, "test");
    assertEquals(-1, shortcutsAdded);
        
    Mockito.verify(mocked.client, Mockito.never()).callHttp(anyString(),
        any(),
        anyString(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), eq(Shortcut.class));
    
  }
  
  public static ProjectData getTestProject(String name) {
    ProjectData apiInput = new ProjectData();
    BasicUser admin = new BasicUser(null, "testuser", "test user");

    apiInput.admins = new ArrayList<>();
    apiInput.admins.add(admin);
    apiInput.name = name;
    apiInput.description = "Test Description";
    apiInput.key = "TESTP";
    apiInput.admin = "Clemens";
    return apiInput;
  }

  private FullJiraProject getReturnProject() {
    return new FullJiraProject(URI.create("http://localhost"), "TESTP", null, null, null, null,
        null, null, null, null, null, null, null);
  }
}
