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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

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
import java.util.function.Function;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Brokmeier, Pascal */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@ActiveProfiles("utest")
public class JiraAdapterTests extends AbstractBaseServiceAdapterTest {

  private static final Logger logger = LoggerFactory.getLogger(JiraAdapterTests.class);

  public static final String TEST_USER_NAME = "testUserName";
  public static final String TEST_USER_PASSWORD = "testUserPassword";
  public static final String TEST_DATA_FILE_JIRA_GET_MYPERMISSIONS_TEMPLATE =
      "jira-get-mypermissions-template";

  private ObjectMapper objectMapper;

  private static TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  @MockBean private IODSAuthnzAdapter authnzAdapter;

  @Mock private CrowdUserDetailsService service;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Autowired @InjectMocks private JiraAdapter jiraAdapter;

  @Autowired private Environment env;

  @Autowired
  @Qualifier("projectTemplateKeyNames")
  private List<String> projectTemplateKeyNames;

  @Before
  public void initTests() {
    objectMapper = new ObjectMapper();

    when(authnzAdapter.getUserName()).thenReturn(TEST_USER_NAME);
    when(authnzAdapter.getUserPassword()).thenReturn(TEST_USER_PASSWORD);
  }

  @Test
  public void createJiraProjectForProject() throws Exception {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);

    assertEquals("10000", spyAdapter.getJiraNotificationSchemeId());

    // delete in case it already exists
    String name = "TestProject";
    String crowdCookieValue = "xyz";

    CrowdUserDetails details = mock(CrowdUserDetails.class);
    Authentication authentication = mock(Authentication.class);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // get authentication mock
    when(authentication.getPrincipal()).thenReturn(details);

    when(service.loadUserByToken(crowdCookieValue)).thenReturn(details);

    when(details.getUsername()).thenReturn("achmed");
    when(details.getFullName()).thenReturn("achmed meyer");

    mockExecute(
            matchesClientCall()
                .method(HttpMethod.POST)
                .bodyMatches(Matchers.instanceOf(FullJiraProject.class)))
        .thenReturn(getReturnProject());

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
        .getEnvironment()
        .getSystemProperties()
        .put("jira.project.template.key.newTemplate", "templateKey");
    spyAdapter
        .getEnvironment()
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

      mockExecute(
              matchesClientCall()
                  .method(HttpMethod.POST)
                  .bodyMatches(Matchers.instanceOf(FullJiraProject.class)))
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
    String existingKey = "TE1";

    List<JsonNode> returnValue = createResult(existingKey);
    mockExecute(
            matchesClientCall()
                .url(containsString("/rest/api/latest/project"))
                .bodyMatches(nullValue())
                .method(HttpMethod.GET))
        .thenReturn(returnValue);

    boolean exists = jiraAdapter.projectKeyExists(existingKey);
    assertThat("expecting key " + existingKey + " exists", exists, CoreMatchers.equalTo(true));
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

    FullJiraProject inputProject = new FullJiraProject();

    mockExecute(
            matchesClientCall()
                .url(containsString("/rest/api/latest/project"))
                .bodyEqualTo(inputProject)
                .method(HttpMethod.POST))
        .thenReturn(expectedProject);

    LeanJiraProject object = spyAdapter.callJiraCreateProjectApi(inputProject);
    assertNotNull(object);
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
        .getEnvironment()
        .getSystemProperties()
        .put("jira.project.template.key.newTemplate", "template");
    jiraAdapter
        .getEnvironment()
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
  public void testCreatePermissions() throws Exception {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);

    String projectNameTrue = "TESTP";
    OpenProjectData apiInput = getTestProject(projectNameTrue);
    apiInput.projectKey = projectNameTrue;

    apiInput.projectAdminUser = "Clemens";
    apiInput.projectAdminGroup = "AdminGroup";
    apiInput.projectUserGroup = "UserGroup";
    apiInput.projectReadonlyGroup = "ReadonlyGroup";

    PermissionScheme scheme = new PermissionScheme();
    scheme.setId("permScheme1");

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(scheme);
    int updates = mocked.createSpecialPermissions(apiInput);

    verifyExecute(
        matchesClientCall()
            .url(containsString("/rest/api/latest/permissionscheme"))
            .method(HttpMethod.POST));
    // update the project

    RestClientCallArgumentMatcher wantedArgument =
        matchesClientCall()
            .url(containsString("/rest/api/latest/project/TESTP/permissionscheme"))
            .method(HttpMethod.PUT);
    verifyExecute(wantedArgument);
    assertEquals(1, updates);
  }

  @Test
  public void testCreateShortcuts() throws Exception {
    JiraAdapter mocked = Mockito.spy(jiraAdapter);

    OpenProjectData apiInput = getTestProject("testproject");

    int shortcutsAdded = mocked.addShortcutsToProject(apiInput);

    assertEquals(5, shortcutsAdded);

    verifyExecute(
        matchesClientCall()
            .url(containsString("/rest/projects/1.0/project/TESTP/shortcut"))
            .method(HttpMethod.POST),
        5);
  }

  @Test
  public void testCreateShortcutsWhenBugtrackerDeactivated() throws Exception {
    OpenProjectData apiInput = getTestProject("testproject");
    apiInput.bugtrackerSpace = false;

    int shortcutsAdded = jiraAdapter.addShortcutsToProject(apiInput);
    assertEquals(-1, shortcutsAdded);

    verifyExecute(matchesClientCall().method(HttpMethod.POST), never());
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

    // see https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10#reported-problems
    Map<String, Map<String, List<Link>>> repos =
        (Map<String, Map<String, List<Link>>>)
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

    Map<String, String> created =
        mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());

    verifyExecute(
        matchesClientCall()
            .url(containsString("/rest/api/latest/component"))
            .method(HttpMethod.POST)
            .returnType(nullValue()));
    assertEquals(1, created.size());
    Map.Entry<String, String> entry = created.entrySet().iterator().next();

    assertEquals("Technology-fe-angular", entry.getKey());
    assertTrue(entry.getValue().contains("https://bb/projects/test/repos/test-fe-angular/browse"));

    // test with uppercase projects
    apiInput = getTestProject("Ai00000001");
    apiInput.projectKey = "Ai00000001";
    apiInput.repositories = openDataRepo;

    created = mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());
    assertEquals(1, created.size());
    entry = created.entrySet().iterator().next();
    assertEquals("Technology-fe-angular", entry.getKey());

    // test with exclude
    List<String> excludes = new ArrayList<>();
    excludes.add("ai00000001-fe-angular");
    created = mocked.createComponentsForProjectRepositories(apiInput, excludes);
    assertEquals(0, created.size());

    // test with null
    created = mocked.createComponentsForProjectRepositories(apiInput, null);
    assertEquals(1, created.size());
    entry = created.entrySet().iterator().next();
    assertEquals("Technology-fe-angular", entry.getKey());

    // test with component creation == false
    mocked.setCreateJiraComponents(false);
    created = mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());
    assertEquals(0, created.size());
  }

  private LeanJiraProject getReturnProject() {
    return new LeanJiraProject(
        URI.create("http://localhost"), "TESTP", null, null, null, null, null);
  }

  @Test
  public void whenHandleExceptionWithinCheckCreateProjectPreconditionsThenException()
      throws IOException {

    jiraAdapter.setRestClient(restClient);
    jiraAdapter.useTechnicalUser = true;
    jiraAdapter.userName = TEST_USER_NAME;
    jiraAdapter.userPassword = TEST_USER_PASSWORD;

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);

    OpenProjectData project = new OpenProjectData();
    project.projectKey = "PKEY";

    IOException ioException = new IOException("throw in unit test");

    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      Assert.assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      Assert.assertTrue(e.getMessage().contains(JiraAdapter.ADAPTER_NAME));
      Assert.assertTrue(e.getMessage().contains(project.projectKey));
    }

    NullPointerException npe = new NullPointerException("npe throw in unit test");
    try {
      when(restClient.execute(isNotNull())).thenThrow(npe);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      Assert.assertTrue(e.getMessage().contains("Unexpected error"));
      Assert.assertTrue(e.getMessage().contains(project.projectKey));
    }
  }

  @Test
  public void testProjectAdminUserExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();

    String user = jiraAdapter.resolveProjectAdminUser(project);

    String response =
        fileReader.readFileContent("jira-get-user-template").replace("<%USERNAME%>", user);

    Function<List<String>, List<String>> checkUser =
        spyAdapter.createProjectAdminUserExistsCheck(user);
    assertNotNull(checkUser);

    List<String> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkUser.apply(result);
      fail();
    } catch (Exception e) {
      Assert.assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkUser.apply(result);
      fail();
    } catch (AdapterException e) {
      Assert.assertEquals(ioException, e.getCause());
    }

    // Case no error, user exists!
    when(restClient.execute(isNotNull())).thenReturn(response);
    List<String> newResult = checkUser.apply(result);
    Assert.assertEquals(0, newResult.size());

    // Case no error, username in json response is different than key but equals to emailAddress,
    // user exists!
    when(restClient.execute(isNotNull())).thenReturn(response);
    newResult = spyAdapter.createProjectAdminUserExistsCheck(user + "@domain.com").apply(result);
    Assert.assertEquals(0, newResult.size());

    // Case error, user does not exists!
    String thisUserDoesNotExists = "this_cd_user_not_exist";
    checkUser = spyAdapter.createProjectAdminUserExistsCheck(thisUserDoesNotExists);
    newResult = checkUser.apply(result);
    Assert.assertEquals(1, newResult.size());
    Assert.assertTrue(newResult.get(0).contains(thisUserDoesNotExists));
  }

  @Test
  public void testRequireGroupExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();

    Function<List<String>, List<String>> checkGroupExists =
        spyAdapter.createRequiredGroupExistsCheck(project);
    assertNotNull(checkGroupExists);

    List<String> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkGroupExists.apply(result);
      fail();
    } catch (Exception e) {
      Assert.assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkGroupExists.apply(result);
      fail();
    } catch (AdapterException e) {
      Assert.assertEquals(ioException, e.getCause());
    }

    // Case no error, group exists!
    String group = jiraAdapter.getGlobalKeyuserRoleName();
    String response =
        fileReader.readFileContent("jira-get-group-template").replace("<%GROUP%>", group);

    when(restClient.execute(isNotNull())).thenReturn(response);
    List<String> newResult = checkGroupExists.apply(result);
    Assert.assertEquals(0, newResult.size());

    // Case error, user does not exists!
    String thisGroupDoesNotExists = "this_group_does_not_exists";
    checkGroupExists = spyAdapter.createProjectAdminUserExistsCheck(thisGroupDoesNotExists);
    newResult = checkGroupExists.apply(result);
    Assert.assertEquals(1, newResult.size());
    Assert.assertTrue(newResult.get(0).contains(thisGroupDoesNotExists));
  }

  @Test
  public void testUserCanCreateProjectCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    //    OpenProjectData project = new OpenProjectData();

    Function<List<String>, List<String>> check =
        spyAdapter.createUserCanCreateProjectCheck(jiraAdapter.getUserName());
    assertNotNull(check);

    List<String> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      check.apply(new ArrayList<>());
      fail();
    } catch (Exception e) {
      Assert.assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException("throws in jira unit test!");
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      check.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      Assert.assertEquals(ioException, e.getCause());
    }

    // Case have and not permission!
    String group = jiraAdapter.getGlobalKeyuserRoleName();
    List.of("true", "false")
        .forEach(
            expected -> {
              try {
                String response =
                    fileReader
                        .readFileContent(TEST_DATA_FILE_JIRA_GET_MYPERMISSIONS_TEMPLATE)
                        .replace("<%HAVE_PERMISSION%>", expected);

                when(restClient.execute(isNotNull())).thenReturn(response);
                List<String> newResult = check.apply(new ArrayList<>());

                Assert.assertEquals("true".equals(expected) ? 0 : 1, newResult.size());

              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

    // Case empty json returned!
    String response =
        fileReader
            .readFileContent(TEST_DATA_FILE_JIRA_GET_MYPERMISSIONS_TEMPLATE)
            .replace("ADMINISTER", "NOTAPERMISSION")
            .replace("<%HAVE_PERMISSION%>", "false");

    when(restClient.execute(isNotNull())).thenReturn(response);
    List<String> newResult = check.apply(new ArrayList<>());
    Assert.assertEquals(1, newResult.size());
  }
}
