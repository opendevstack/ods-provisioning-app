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

package org.opendevstack.provision.services;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.jira.Component;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.model.jira.Shortcut;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.opendevstack.provision.util.exception.HttpException;
import org.opendevstack.provision.util.rest.Client;
import org.opendevstack.provision.util.rest.ClientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Brokmeier, Pascal */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class JiraAdapterTests {

  @Mock CrowdUserDetailsService service;

  List<LeanJiraProject> projects = new ArrayList<>();

  @Mock RestClient restClient;

  @Mock Client client;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired @InjectMocks JiraAdapter jiraAdapter;

  @Autowired Environment env;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Autowired List<String> projectTemplateKeyNames;
  private ObjectMapper objectMapper;

  @Before
  public void initTests() {
    MockitoAnnotations.initMocks(this);
    projects = new ArrayList<>();
    objectMapper = new ObjectMapper();
  }

  @Test
  public void createJiraProjectForProject() throws Exception {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);

    assertEquals("10000", spyAdapter.jiraNotificationSchemeId);

    // delete in case it already exists
    String name = "TestProject";
    String crowdCookieValue = "xyz";

    CrowdUserDetails details = mock(CrowdUserDetails.class);
    Authentication authentication = mock(Authentication.class);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // get authentication mock
    when(authentication.getPrincipal()).thenReturn(details);

    when(service.loadUserByToken(crowdCookieValue)).thenReturn(details);

    Mockito.doNothing().when(restClient).getSessionId(null);

    when(details.getUsername()).thenReturn("achmed");
    when(details.getFullName()).thenReturn("achmed meyer");

    when(restClient.callHttp(
            anyString(),
            any(FullJiraProject.class),
            anyBoolean(),
            eq(RestClient.HTTP_VERB.POST),
            eq(LeanJiraProject.class)))
        .thenReturn(getReturnProject());

    ClientCall clientCall = mock(ClientCall.class);
    when((client.post())).thenReturn(clientCall);
    when(clientCall.execute()).thenReturn(getReturnProject());

    OpenProjectData createdProject =
        spyAdapter.createBugtrackerProjectForODSProject(getTestProject(name));

    assertEquals(getTestProject(name).projectKey, createdProject.projectKey);
    assertEquals(getTestProject(name).projectName, createdProject.projectName);
    // default template
    assertEquals(defaultProjectKey, createdProject.projectType);

    // new template
    OpenProjectData templateProject = getTestProject(name);
    templateProject.projectType = "newTemplate";

    projectTemplateKeyNames.add("newTemplate");
    spyAdapter
        .environment
        .getSystemProperties()
        .put("jira.project.template.key.newTemplate", "templateKey");
    spyAdapter
        .environment
        .getSystemProperties()
        .put("jira.project.template.type.newTemplate", "templateType");

    OpenProjectData createdProjectWithNewTemplate =
        spyAdapter.createBugtrackerProjectForODSProject(templateProject);

    assertEquals(templateProject.projectKey, createdProjectWithNewTemplate.projectKey);
    assertEquals(templateProject.projectName, createdProjectWithNewTemplate.projectName);
    assertEquals("newTemplate", createdProjectWithNewTemplate.projectType);

    HttpException thrownEx = null;
    try {
      HttpException ioEx = new HttpException(300, "testerror");

      when(restClient.callHttp(
              anyString(),
              any(FullJiraProject.class),
              anyBoolean(),
              any(RestClient.HTTP_VERB.class),
              eq(LeanJiraProject.class)))
          .thenThrow(ioEx);

      spyAdapter.createBugtrackerProjectForODSProject(getTestProject(name));
    } catch (HttpException e) {
      thrownEx = e;
    }
    assertNotNull(thrownEx);
    assertEquals(300, thrownEx.getResponseCode());
  }

  @Test
  public void projectKeyExists() throws IOException {

    // List<JsonNode> projects =
    //        httpGet().url(url).returnTypeReference(new TypeReference<List<JsonNode>>()
    // {}).execute();
    String existingKey = "TE1";

    ClientCall clientCall = mock(ClientCall.class,Mockito.RETURNS_DEEP_STUBS);
    when(clientCall
        .basicAuthenticated(any())
        .url(anyString())
        .returnTypeReference(any())
        .execute())
        .thenReturn(createResult(existingKey));

    when(client.call(eq(HTTP_VERB.GET))).thenReturn(clientCall);

    boolean exists = jiraAdapter.projectKeyExists(existingKey);
    assertThat(        "expecting key " + existingKey + " exists", exists, CoreMatchers.<Boolean>equalTo(true));
  }

  public List<JsonNode> createResult(String existingKey) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.set("key", new TextNode(existingKey));

    return asList(objectNode);
  }

  @Test
  public void callJiraCreateProjectApi() throws IOException {
    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    LeanJiraProject expectedProject = new LeanJiraProject();
    expectedProject.key = "1";

    when(restClient.callHttp(
            anyString(),
            any(FullJiraProject.class),
            anyBoolean(),
            eq(RestClient.HTTP_VERB.POST),
            eq(LeanJiraProject.class)))
        .thenReturn(expectedProject);

    assertNotNull(spyAdapter.callJiraCreateProjectApi(new FullJiraProject()));
  }

  @Test
  public void buildJiraProjectPojoFromApiProject() throws Exception {
    OpenProjectData apiInput = getTestProject("TestProject");
    apiInput.projectKey = "TestP";

    FullJiraProject fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);

    assertEquals(apiInput.projectName, fullJiraProject.getName());
    assertEquals(apiInput.projectKey, fullJiraProject.getKey());
    assertEquals(env.getProperty("jira.project.template.key"), fullJiraProject.projectTemplateKey);
    assertEquals(env.getProperty("jira.project.template.type"), fullJiraProject.projectTypeKey);

    apiInput.projectType = "notFound";
    fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    assertEquals(env.getProperty("jira.project.template.key"), fullJiraProject.projectTemplateKey);
    assertEquals(env.getProperty("jira.project.template.type"), fullJiraProject.projectTypeKey);

    apiInput.projectType = "newTemplate";

    // set them adhoc
    projectTemplateKeyNames.add("newTemplate");

    jiraAdapter
        .environment
        .getSystemProperties()
        .put("jira.project.template.key.newTemplate", "template");
    jiraAdapter
        .environment
        .getSystemProperties()
        .put("jira.project.template.type.newTemplate", "templateType");
    fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    assertEquals("template", fullJiraProject.projectTemplateKey);
    assertEquals("templateType", fullJiraProject.projectTypeKey);

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
  }

  @Test
  public void buildProjectKey() {
    String shortName = "shrt";

    assertEquals("SHRT", jiraAdapter.buildProjectKey(shortName));
  }

  @Test
  public void projectExists() throws Exception {
    String projectNameTrue = "TESTP";
    String projectNameFalse = "TESTP_FALSE";

    OpenProjectData apiInput = getTestProject(projectNameTrue);
    apiInput.projectKey = projectNameTrue;

    LeanJiraProject fullJiraProject = jiraAdapter.buildJiraProjectPojoFromApiProject(apiInput);
    projects.add(fullJiraProject);
    Map<String, String> keys = jiraAdapter.convertJiraProjectToKeyMap(projects);

    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    Mockito.doNothing().when(restClient).getSessionId(null);
    Mockito.doReturn(keys).when(mocked).getProjects(projectNameTrue);

    assertTrue(mocked.projectKeyExists(projectNameTrue));

    projects.clear();
    Mockito.doReturn(keys).when(mocked).getProjects(projectNameFalse);
    assertFalse(mocked.projectKeyExists(projectNameFalse));
  }

  @Test
  public void testCreatePermissions() throws Exception {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    Mockito.doNothing().when(restClient).getSessionId(null);

    String projectNameTrue = "TESTP";
    OpenProjectData apiInput = getTestProject(projectNameTrue);
    apiInput.projectKey = projectNameTrue;

    apiInput.projectAdminUser = "Clemens";
    apiInput.projectAdminGroup = "AdminGroup";
    apiInput.projectUserGroup = "UserGroup";
    apiInput.projectReadonlyGroup = "ReadonlyGroup";

    PermissionScheme scheme = new PermissionScheme();
    scheme.setId("permScheme1");

    when(restClient.callHttp(
            anyString(),
            any(),
            anyBoolean(),
            eq(RestClient.HTTP_VERB.POST),
            eq(PermissionScheme.class)))
        .thenReturn(scheme);

    int updates = mocked.createSpecialPermissions(apiInput);

    // create the permission scheme
    Mockito.verify(restClient, Mockito.times(1))
        .callHttp(
            anyString(),
            any(),
            anyBoolean(),
            eq(RestClient.HTTP_VERB.POST),
            eq(PermissionScheme.class));

    // update the project
    Mockito.verify(restClient, Mockito.times(1))
        .callHttp(anyString(), any(), anyBoolean(), eq(RestClient.HTTP_VERB.PUT), eq(null));

    assertEquals(1, updates);
  }

  @Test
  public void testCreateShortcuts() throws Exception {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    RestClient restClientMocket = Mockito.spy(RestClient.class);
    OpenProjectData apiInput = getTestProject("testproject");

    int shortcutsAdded = mocked.addShortcutsToProject(apiInput);

    assertEquals(5, shortcutsAdded);

    Mockito.verify(restClient, Mockito.times(5))
        .callHttp(
            anyString(), any(), anyBoolean(), eq(RestClient.HTTP_VERB.POST), eq(Shortcut.class));

    apiInput.bugtrackerSpace = false;
    mocked = Mockito.spy(jiraAdapter);
    mocked.client = Mockito.spy(Client.class);
    shortcutsAdded = mocked.addShortcutsToProject(apiInput);
    assertEquals(-1, shortcutsAdded);

    Mockito.verify(mocked.client, Mockito.never()).call(HTTP_VERB.POST);
  }

  public static OpenProjectData getTestProject(String name) {
    OpenProjectData apiInput = new OpenProjectData();
    apiInput.projectName = name;
    apiInput.description = "Test Description";
    apiInput.projectKey = "TESTP";
    apiInput.projectAdminUser = "Clemens";
    return apiInput;
  }

  @Test
  public void testComponentCreation() throws Exception {
    TypeReference reference = new TypeReference<Map<String, Map<String, List<Link>>>>() {};

    Map<String, Map<String, List<Link>>> repos =
        new ObjectMapper()
            .readValue(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("data/repositoryTestData.txt"),
                reference);

    String repoName = "ai00000001-fe-angular";

    RepositoryData legacyRepoData = new RepositoryData();
    legacyRepoData.setName(repoName);
    legacyRepoData.setLinks(repos.get(repoName));

    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    OpenProjectData apiInput = getTestProject("ai00000001");
    apiInput.projectKey = "ai00000001";

    Map<String, Map<URL_TYPE, String>> openDataRepo = new HashMap<>();

    openDataRepo.put(legacyRepoData.getName(), legacyRepoData.convertRepoToOpenDataProjectRepo());
    apiInput.repositories = openDataRepo;

    Map<String, String> created = mocked.createComponentsForProjectRepositories(apiInput);

    Mockito.verify(restClient, Mockito.times(1))
        .callHttp(
            endsWith("/rest/api/latest/component"),
            any(Component.class),
            anyBoolean(),
            eq(RestClient.HTTP_VERB.POST),
            isNull());

    assertEquals(1, created.size());
    Map.Entry<String, String> entry = created.entrySet().iterator().next();

    assertEquals("Technology-fe-angular", entry.getKey());
    assertTrue(entry.getValue().contains("https://bb/projects/test/repos/test-fe-angular/browse"));

    // test with uppercase projects
    apiInput = getTestProject("Ai00000001");
    apiInput.projectKey = "Ai00000001";
    apiInput.repositories = openDataRepo;

    created = mocked.createComponentsForProjectRepositories(apiInput);
    assertEquals(1, created.size());
    entry = created.entrySet().iterator().next();
    assertEquals("Technology-fe-angular", entry.getKey());

    // test with component creation == false
    mocked.createJiraComponents = false;
    created = mocked.createComponentsForProjectRepositories(apiInput);
    assertEquals(0, created.size());
  }

  private LeanJiraProject getReturnProject() {
    return new LeanJiraProject(
        URI.create("http://localhost"), "TESTP", null, null, null, null, null);
  }
}
