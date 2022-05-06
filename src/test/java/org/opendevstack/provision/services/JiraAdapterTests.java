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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.LeanJiraProject;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.services.jira.FullJiraProjectFactory;
import org.opendevstack.provision.services.jira.JiraProjectTypePropertyCalculator;
import org.opendevstack.provision.services.jira.JiraRestService;
import org.opendevstack.provision.services.jira.JiraSpecialPermissioner;
import org.opendevstack.provision.services.jira.WebhookProxyJiraPropertyUpdater;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
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

  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;

  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @MockBean private IODSAuthnzAdapter authnzAdapter;

  @MockBean private CrowdUserDetailsService service;

  @MockBean private JiraProjectTypePropertyCalculator jiraProjectTypePropertyCalculator;

  @MockBean private WebhookProxyJiraPropertyUpdater webhookProxyJiraPropertyUpdater;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Autowired private JiraAdapter jiraAdapter;

  @Autowired private JiraSpecialPermissioner jiraSpecialPermissioner;

  @Autowired private FullJiraProjectFactory fullJiraProjectFactory;

  @Autowired
  @Qualifier("projectTemplateKeyNames")
  private List<String> projectTemplateKeyNames;

  @BeforeEach
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

    OpenProjectData project = getTestProject(name);
    project.setProjectType("default");
    project.setWebhookProxySecret(UUID.randomUUID().toString());

    CrowdUserDetails details = mock(CrowdUserDetails.class);
    Authentication authentication = mock(Authentication.class);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // get authentication mock
    when(authentication.getPrincipal()).thenReturn(details);

    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                project.getProjectType(),
                JiraAdapter.ADD_WEBHOOK_PROXY_URL_AS_PROJECT_PROPERTY,
                Boolean.FALSE.toString()))
        .thenReturn(Boolean.TRUE.toString());

    when(service.loadUserByToken(crowdCookieValue)).thenReturn(details);

    when(details.getUsername()).thenReturn("achmed");
    when(details.getFullName()).thenReturn("achmed meyer");

    mockExecute(
            matchesClientCall()
                .method(HttpMethod.POST)
                .bodyMatches(Matchers.instanceOf(FullJiraProject.class)))
        .thenReturn(getReturnProject());

    OpenProjectData createdProject = spyAdapter.createBugtrackerProjectForODSProject(project);

    assertEquals(getTestProject(name).getProjectKey(), createdProject.getProjectKey());
    assertEquals(getTestProject(name).getProjectName(), createdProject.getProjectName());
    // default template
    assertEquals(defaultProjectKey, createdProject.getProjectType());

    // new template
    OpenProjectData templateProject = getTestProject(name);
    templateProject.setProjectType("newTemplate");

    projectTemplateKeyNames.add("newTemplate");

    OpenProjectData createdProjectWithNewTemplate =
        spyAdapter.createBugtrackerProjectForODSProject(templateProject);

    assertEquals(templateProject.getProjectKey(), createdProjectWithNewTemplate.getProjectKey());
    assertEquals(templateProject.getProjectName(), createdProjectWithNewTemplate.getProjectName());
    assertEquals("newTemplate", createdProjectWithNewTemplate.getProjectType());
    verify(webhookProxyJiraPropertyUpdater, times(1))
        .addWebhookProxyProperty(
            spyAdapter,
            project.getProjectKey(),
            project.getProjectType(),
            project.getWebhookProxySecret());

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

    assertTrue(
        jiraAdapter.projectKeyExists(existingKey), "expecting key " + existingKey + " exists");
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
    expectedProject.setKey("1");

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
    apiInput.setProjectKey("TestP");

    // default or null template
    apiInput.setProjectType(null);
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                apiInput.getProjectType(), JiraAdapter.JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey))
        .thenReturn(jiraTemplateKey);
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                apiInput.getProjectType(), JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType))
        .thenReturn(jiraTemplateType);

    FullJiraProject fullJiraProject =
        fullJiraProjectFactory.buildJiraProjectPojoFromApiProject(apiInput);

    assertEquals(apiInput.getProjectName(), fullJiraProject.getName());
    assertEquals(apiInput.getProjectKey(), fullJiraProject.getKey());
    assertEquals(jiraTemplateKey, fullJiraProject.getProjectTemplateKey());
    assertEquals(jiraTemplateType, fullJiraProject.getProjectTypeKey());
    assertEquals(apiInput.getProjectType(), defaultProjectKey);

    // not default template
    apiInput.setProjectType("not-default");
    String notDefaultTemplateKey = "projectTypeNotDefaultTemplateKey";
    String notDefaultTemplateType = "projectTypeNotDefaultTemplateType";
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                apiInput.getProjectType(), JiraAdapter.JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey))
        .thenReturn(notDefaultTemplateKey);
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                apiInput.getProjectType(), JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType))
        .thenReturn(notDefaultTemplateType);

    assertEquals(apiInput.getProjectName(), fullJiraProject.getName());
    assertEquals(apiInput.getProjectKey(), fullJiraProject.getKey());
    assertEquals(jiraTemplateKey, fullJiraProject.getProjectTemplateKey());
    assertEquals(jiraTemplateType, fullJiraProject.getProjectTypeKey());
    assertNotEquals(apiInput.getProjectType(), defaultProjectKey);
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
    apiInput.setProjectKey(projectNameTrue);

    apiInput.setProjectAdminUser("Clemens");
    apiInput.setProjectAdminGroup("AdminGroup");
    apiInput.setProjectUserGroup("UserGroup");
    apiInput.setProjectReadonlyGroup("ReadonlyGroup");

    PermissionScheme scheme = new PermissionScheme();
    scheme.setId("permScheme1");

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(scheme);
    jiraSpecialPermissioner.setupProjectSpecialPermissions(mocked, apiInput);

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
  }

  public static OpenProjectData getTestProject(String name) {
    OpenProjectData apiInput = new OpenProjectData();
    apiInput.setProjectName(name);
    apiInput.setDescription("Test Description");
    apiInput.setProjectKey("TESTP");
    apiInput.setProjectAdminUser("Clemens");
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

    String repoName = "ai00000001-fe-angular-ai00000001";

    RepositoryData legacyRepoData = new RepositoryData();
    legacyRepoData.setName(repoName);
    legacyRepoData.setLinks(repos.get(repoName));

    JiraAdapter mocked = Mockito.spy(jiraAdapter);
    OpenProjectData apiInput = getTestProject("ai00000001");
    apiInput.setProjectKey("ai00000001");

    Map<String, Map<URL_TYPE, String>> openDataRepo = new HashMap<>();

    openDataRepo.put(legacyRepoData.getName(), legacyRepoData.convertRepoToOpenDataProjectRepo());
    apiInput.setRepositories(openDataRepo);

    Map<String, String> created =
        mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());

    verifyExecute(
        matchesClientCall()
            .url(containsString("/rest/api/latest/component"))
            .method(HttpMethod.POST)
            .returnType(nullValue()));
    assertEquals(1, created.size());
    Map.Entry<String, String> entry = created.entrySet().iterator().next();

    assertEquals("Technology-fe-angular-ai00000001", entry.getKey());
    assertTrue(
        entry
            .getValue()
            .contains("https://bb/projects/test/repos/test-fe-angular-ai00000001/browse"));

    // test with uppercase projects
    apiInput = getTestProject("Ai00000001");
    apiInput.setProjectKey("Ai00000001");
    apiInput.setRepositories(openDataRepo);

    created = mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());
    assertEquals(1, created.size());
    entry = created.entrySet().iterator().next();
    assertEquals("Technology-fe-angular-ai00000001", entry.getKey());

    // test with exclude
    List<String> excludes = new ArrayList<>();
    excludes.add("ai00000001-fe-angular-ai00000001");
    created = mocked.createComponentsForProjectRepositories(apiInput, excludes);
    assertEquals(0, created.size());

    // test with null
    created = mocked.createComponentsForProjectRepositories(apiInput, null);
    assertEquals(1, created.size());
    entry = created.entrySet().iterator().next();
    assertEquals("Technology-fe-angular-ai00000001", entry.getKey());

    // test with component creation == false
    mocked.setCreateJiraComponents(false);
    created = mocked.createComponentsForProjectRepositories(apiInput, new ArrayList<>());
    assertEquals(0, created.size());
  }

  private LeanJiraProject getReturnProject() {
    return new LeanJiraProject(
        URI.create("http://localhost"), "TESTP", null, null, null, null, null, null);
  }

  @Test
  public void whenHandleExceptionWithinCheckCreateProjectPreconditionsThenException()
      throws IOException {

    jiraAdapter.setRestClient(restClient);
    jiraAdapter.setUseTechnicalUser(true);
    jiraAdapter.setUserName(TEST_USER_NAME);
    jiraAdapter.setUserPassword(TEST_USER_PASSWORD);

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("PKEY");

    IOException ioException = new IOException("throw in unit test");

    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      assertTrue(e.getMessage().contains(JiraAdapter.ADAPTER_NAME));
      assertTrue(e.getMessage().contains(project.getProjectKey()));
    }

    NullPointerException npe = new NullPointerException("npe throw in unit test");
    try {
      when(restClient.execute(isNotNull())).thenThrow(npe);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getMessage().contains("Unexpected error"));
      assertTrue(e.getMessage().contains(project.getProjectKey()));
    }
  }

  @Test
  public void testProjectAdminUserExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();

    String user = jiraAdapter.resolveProjectAdminUser(project);

    String response =
        fileReader
            .readFileContent("jira-get-user-template")
            .replace("<%USERNAME%>", user.toUpperCase());

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkUser =
        spyAdapter.createProjectAdminUserExistsCheck(user);
    assertNotNull(checkUser);

    List<CheckPreconditionFailure> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkUser.apply(result);
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkUser.apply(result);
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case no error, user exists!
    when(restClient.execute(isNotNull())).thenReturn(response);
    List<CheckPreconditionFailure> newResult = checkUser.apply(result);
    assertEquals(0, newResult.size());

    // Case no error, username in json response is different than key but equals to emailAddress,
    // user exists!
    when(restClient.execute(isNotNull())).thenReturn(response);
    newResult = spyAdapter.createProjectAdminUserExistsCheck(user + "@domain.com").apply(result);
    assertEquals(0, newResult.size());

    // Case error, user does not exists!
    String thisUserDoesNotExists = "this_cd_user_not_exist";
    checkUser = spyAdapter.createProjectAdminUserExistsCheck(thisUserDoesNotExists);
    newResult = checkUser.apply(result);
    assertEquals(1, newResult.size());
    assertTrue(newResult.get(0).toString().contains(thisUserDoesNotExists));
  }

  @Test
  public void testPermissionSchemeExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();
    project.setSpecialPermissionSchemeId(Integer.valueOf(10100));
    project.setSpecialPermissionSet(true);

    String response = fileReader.readFileContent("jira-get-permission-scheme-by-id");

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkPermissionScheme =
        spyAdapter.createSpecialPermissionSchemeIdCheck(project);
    assertNotNull(checkPermissionScheme);

    List<CheckPreconditionFailure> result;

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkPermissionScheme.apply(new ArrayList<>());
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkPermissionScheme.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case no error, permission scheme id exists!
    when(restClient.execute(isNotNull())).thenReturn(response);
    result = checkPermissionScheme.apply(new ArrayList<>());
    assertEquals(0, result.size());

    // Case project is not enabled with special permission set
    project.setSpecialPermissionSet(false);
    when(restClient.execute(isNotNull())).thenReturn(response);
    result = checkPermissionScheme.apply(new ArrayList<>());
    assertEquals(0, result.size());

    // Case error, permission schemed does not exists!
    project.setSpecialPermissionSet(true);
    Integer unexistantSchemeId = Integer.valueOf(99999);
    project.setSpecialPermissionSchemeId(unexistantSchemeId);
    when(restClient.execute(isNotNull()))
        .thenThrow(
            new HttpException(
                404,
                "Permission scheme " + project.getSpecialPermissionSchemeId() + " does not exist"));
    result = checkPermissionScheme.apply(new ArrayList<>());
    assertEquals(1, result.size());
    assertTrue(result.get(0).toString().contains(unexistantSchemeId.toString()));
  }

  @Test
  public void testRoleExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("TEST");
    project.setSpecialPermissionSchemeId(Integer.valueOf(10100));
    project.setProjectRoleForAdminGroup("10100");
    project.setProjectRoleForUserGroup("10100");
    project.setProjectRoleForReadonlyGroup("10100");
    project.setSpecialPermissionSet(true);

    String response = fileReader.readFileContent("jira-get-role");

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkRole =
        spyAdapter.createRoleExistsCheck(project);
    assertNotNull(checkRole);

    List<CheckPreconditionFailure> result;

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkRole.apply(new ArrayList<>());
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkRole.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case no error, project role id exists!
    reset(restClient);
    when(restClient.execute(isNotNull())).thenReturn(response);
    result = checkRole.apply(new ArrayList<>());
    assertEquals(0, result.size());
    verify(restClient, times(3)).execute(isNotNull());

    // Case project is not enabled with special permission set
    project.setSpecialPermissionSet(false);
    when(restClient.execute(isNotNull())).thenReturn(response);
    result = checkRole.apply(new ArrayList<>());
    assertEquals(0, result.size());

    // Case error: project roles does not exists
    project.setSpecialPermissionSet(true);
    project.setProjectRoleForAdminGroup(null);
    project.setProjectRoleForUserGroup(null);
    project.setProjectRoleForReadonlyGroup(null);
    result = checkRole.apply(new ArrayList<>());
    assertEquals(3, result.size());
    assertTrue(
        result
            .toArray()[0]
            .toString()
            .contains(CheckPreconditionFailure.ExceptionCodes.ROLE_NOT_DEFINED.toString()));

    // Case: using project roles from utest configuration
    project.setSpecialPermissionSet(true);
    project.setProjectRoleForAdminGroup("99999");
    project.setProjectRoleForUserGroup("99999");
    project.setProjectRoleForReadonlyGroup("99999");
    when(restClient.execute(isNotNull())).thenThrow(new HttpException(404, "Not found!"));
    result = checkRole.apply(new ArrayList<>());
    assertEquals(3, result.size());
  }

  @Test
  public void testRequireGroupExistsCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkGroupExists =
        spyAdapter.createRequiredGroupExistsCheck(project);
    assertNotNull(checkGroupExists);

    List<CheckPreconditionFailure> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkGroupExists.apply(result);
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      checkGroupExists.apply(result);
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case no error, group exists!
    String group = jiraAdapter.getGlobalKeyuserRoleName();
    String response =
        fileReader
            .readFileContent("jira-get-group-template")
            .replace("<%GROUP%>", group.toUpperCase());

    when(restClient.execute(isNotNull())).thenReturn(response);
    List<CheckPreconditionFailure> newResult = checkGroupExists.apply(result);
    assertEquals(0, newResult.size());

    // Case error, user does not exists!
    String thisGroupDoesNotExists = "this_group_does_not_exists".toUpperCase();
    checkGroupExists = spyAdapter.createProjectAdminUserExistsCheck(thisGroupDoesNotExists);
    newResult = checkGroupExists.apply(result);
    assertEquals(1, newResult.size());
    assertTrue(newResult.get(0).toString().contains(thisGroupDoesNotExists));
  }

  @Test
  public void testUserCanCreateProjectCheck() throws IOException {

    JiraAdapter spyAdapter = Mockito.spy(jiraAdapter);
    spyAdapter.setRestClient(restClient);

    //    OpenProjectData project = new OpenProjectData();

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> check =
        spyAdapter.createUserCanCreateProjectCheck(jiraAdapter.getUserName());
    assertNotNull(check);

    List<CheckPreconditionFailure> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      check.apply(new ArrayList<>());
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException("throws in jira unit test!");
    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);
      check.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case have and not permission!
    List.of("true", "false")
        .forEach(
            expected -> {
              try {
                String response =
                    fileReader
                        .readFileContent(TEST_DATA_FILE_JIRA_GET_MYPERMISSIONS_TEMPLATE)
                        .replace("<%HAVE_PERMISSION%>", expected);

                when(restClient.execute(isNotNull())).thenReturn(response);
                List<CheckPreconditionFailure> newResult = check.apply(new ArrayList<>());

                assertEquals("true".equals(expected) ? 0 : 1, newResult.size());

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
    List<CheckPreconditionFailure> newResult = check.apply(new ArrayList<>());
    assertEquals(1, newResult.size());
  }

  @Test
  public void givenAddWebhookProxyUrlToJiraProject_whenProjectTypeNotEnabled_doNotAddProperty()
      throws IOException {

    String projectType = "type";
    String projectKey = "projectKey";
    String secret = "secret";

    // case: configured property can be parse to "false"
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                projectType,
                JiraAdapter.ADD_WEBHOOK_PROXY_URL_AS_PROJECT_PROPERTY,
                Boolean.FALSE.toString()))
        .thenReturn(Boolean.FALSE.toString());

    jiraAdapter.addWebhookProxyUrlToJiraProject(projectKey, projectType, secret);

    verify(webhookProxyJiraPropertyUpdater, never())
        .addWebhookProxyProperty(any(JiraRestService.class), anyString(), anyString(), anyString());

    // case: configured property can not be parse to "false"
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                projectType,
                JiraAdapter.ADD_WEBHOOK_PROXY_URL_AS_PROJECT_PROPERTY,
                Boolean.FALSE.toString()))
        .thenReturn("can-be-not-parsed-to-boolean");

    jiraAdapter.addWebhookProxyUrlToJiraProject(projectKey, projectType, secret);

    verify(webhookProxyJiraPropertyUpdater, never())
        .addWebhookProxyProperty(any(JiraRestService.class), anyString(), anyString(), anyString());
  }

  @Test
  public void givenAddWebhookProxyUrlToJiraProject_whenProjectTypeIsEnabled_addProperty()
      throws IOException {

    String projectType = "type";
    String projectKey = "projectKey";
    String secret = "secret";

    // case: configured property can be parse to "false"
    when(jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                projectType,
                JiraAdapter.ADD_WEBHOOK_PROXY_URL_AS_PROJECT_PROPERTY,
                Boolean.FALSE.toString()))
        .thenReturn(Boolean.TRUE.toString());

    jiraAdapter.addWebhookProxyUrlToJiraProject(projectKey, projectType, secret);

    verify(webhookProxyJiraPropertyUpdater, times(1))
        .addWebhookProxyProperty(any(JiraRestService.class), anyString(), anyString(), anyString());
  }
}
