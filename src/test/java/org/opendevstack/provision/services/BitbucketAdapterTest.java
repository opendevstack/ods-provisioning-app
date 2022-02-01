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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_ID_KEY;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_TYPE_KEY;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.*;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.ValueCaptor;
import org.opendevstack.provision.util.exception.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@ActiveProfiles({"utest", "quickstarters"})
public class BitbucketAdapterTest extends AbstractBaseServiceAdapterTest {

  public static final String TEST_COMPONENT_ID_KEY = "testid";
  public static final String TEST_COMPONENT_DESCRIPTION = "test component description";
  public static final String TEST_DEFAULT_ADMIN_GROUP = "opendevstack-administrators";
  public static final String TEST_DEFAULT_USER_GROUP = "opendevstack-users";

  @Value("${openshift.jenkins.project.webhookproxy.events}")
  private List<String> webhookEvents;

  public static final String TEST_USER_NAME = "testUserName";
  public static final String TEST_USER_PASSWORD = "testUserPassword";

  @MockBean private IODSAuthnzAdapter authnzAdapter;
  @Autowired private BitbucketAdapter bitbucketAdapter;

  private static final TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  @BeforeEach
  public void initTests() {
    when(authnzAdapter.getUserName()).thenReturn(TEST_USER_NAME);
    when(authnzAdapter.getUserPassword()).thenReturn(TEST_USER_PASSWORD);
  }

  @Test
  public void createSCMProjectForODSProject() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    Map<String, List<Link>> map = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("testname");
    link.setHref("testlink");
    linkList.add(link);
    map.put("self", linkList);
    BitbucketProjectData bitbucketData = getReturnBitbucketData();
    bitbucketData.setLinks(map);

    doReturn(bitbucketData).when(spyAdapter).callCreateProjectApi(any(OpenProjectData.class));

    String scmUrl = spyAdapter.createSCMProjectForODSProject(getReturnOpenProjectData());

    assertEquals("testlink", scmUrl);
  }

  @Test
  public void createComponentRepositoriesForODSProject() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);
    OpenProjectData projectData = getReturnOpenProjectData();
    RepositoryData repoData = getReturnRepoData();

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(any(), any(), any());

    doReturn(repoData)
        .when(spyAdapter)
        .callCreateRepoApi(anyString(), anyBoolean(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> result =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    verify(spyAdapter).createWebHooksForRepository(repoData, projectData, "testComponent");
    for (Entry<String, Map<URL_TYPE, String>> entry : result.entrySet()) {
      Map<URL_TYPE, String> resultLinkMap = entry.getValue();
      assertEquals(repoData.convertRepoToOpenDataProjectRepo(), resultLinkMap);
    }
  }

  @Test
  public void createComponentRepositoriesForODSProjectWhenRepositoriesNotEqNull() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    OpenProjectData projectData = getReturnOpenProjectData();
    projectData.setRepositories(new HashMap<>());

    Map<String, String> quickstart = new HashMap<>();
    quickstart.put(OpenProjectData.COMPONENT_ID_KEY, TEST_COMPONENT_ID_KEY);
    quickstart.put(OpenProjectData.COMPONENT_DESC_KEY, TEST_COMPONENT_DESCRIPTION);
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);

    projectData.setQuickstarters(quickstarters);
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    repoData.setName("testRepoName");
    String projectKey = "testkey";
    projectData.setProjectKey(projectKey);

    Mockito.doNothing()
        .when(spyAdapter)
        .createWebHooksForRepository(repoData, projectData, "testComponent");
    doReturn(repoData)
        .when(spyAdapter)
        .callCreateRepoApi(anyString(), anyBoolean(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> result =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    for (Entry<String, Map<URL_TYPE, String>> entry : result.entrySet()) {
      Map<URL_TYPE, String> resultLinkMap = entry.getValue();
      assertEquals(repoData.convertRepoToOpenDataProjectRepo(), resultLinkMap);
    }

    ArgumentCaptor<Repository> argumentCaptor = ArgumentCaptor.forClass(Repository.class);
    verify(spyAdapter, times(1))
        .callCreateRepoApi(eq(projectKey), eq(false), argumentCaptor.capture());
    Repository repo = argumentCaptor.getValue();
    assertEquals(TEST_COMPONENT_DESCRIPTION, repo.getDescription());
    assertNotNull(repo.getName());
    assertEquals(TEST_DEFAULT_ADMIN_GROUP, repo.getAdminGroup());
    assertEquals(TEST_DEFAULT_USER_GROUP, repo.getUserGroup());
  }

  @Test
  public void createComponentRepositoriesForODSProjectWithSpecialPermissionSet() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    OpenProjectData projectData = getReturnOpenProjectData();
    projectData.setRepositories(new HashMap<>());

    Map<String, String> quickstart = new HashMap<>();
    quickstart.put(OpenProjectData.COMPONENT_ID_KEY, TEST_COMPONENT_ID_KEY);
    quickstart.put(OpenProjectData.COMPONENT_DESC_KEY, TEST_COMPONENT_DESCRIPTION);
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);

    projectData.setQuickstarters(quickstarters);
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    repoData.setName("testRepoName");
    String projectKey = "testkey";
    projectData.setProjectKey(projectKey);
    projectData.setSpecialPermissionSet(true);
    projectData.setProjectAdminGroup("projectAdminGroup");
    projectData.setProjectUserGroup("projectUserGroup");

    Mockito.doNothing()
        .when(spyAdapter)
        .createWebHooksForRepository(repoData, projectData, "testComponent");
    doReturn(repoData)
        .when(spyAdapter)
        .callCreateRepoApi(anyString(), anyBoolean(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> result =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    for (Entry<String, Map<URL_TYPE, String>> entry : result.entrySet()) {
      Map<URL_TYPE, String> resultLinkMap = entry.getValue();
      assertEquals(repoData.convertRepoToOpenDataProjectRepo(), resultLinkMap);
    }

    ArgumentCaptor<Repository> argumentCaptor = ArgumentCaptor.forClass(Repository.class);
    verify(spyAdapter, times(1))
        .callCreateRepoApi(eq(projectKey), eq(true), argumentCaptor.capture());
    Repository repo = argumentCaptor.getValue();
    assertEquals(TEST_COMPONENT_DESCRIPTION, repo.getDescription());
    assertNotNull(repo.getName());
    assertEquals(projectData.getProjectAdminGroup(), repo.getAdminGroup());
    assertEquals(projectData.getProjectUserGroup(), repo.getUserGroup());
  }

  @Test
  public void testCreateRepositoriesForProjectWhenQuickstartEqNull() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    OpenProjectData projectData = new OpenProjectData();

    RepositoryData repoData = new RepositoryData();

    Map<String, List<Link>> links = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("http");
    link.setHref("clone");
    linkList.add(link);
    links.put("clone", linkList);
    repoData.setLinks(links);

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(any(), any(), any());
    doReturn(repoData)
        .when(spyAdapter)
        .callCreateRepoApi(anyString(), anyBoolean(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> actual =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    assertEquals(new HashMap<String, Map<URL_TYPE, String>>(), actual);

    verify(spyAdapter, times(0)).callCreateRepoApi(anyString(), anyBoolean(), any());
  }

  @Test
  public void callCreateProjectApiWithPermissionSetTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    OpenProjectData data = new OpenProjectData();
    data.setProjectKey("testkey");
    data.setProjectName("testproject");
    data.setDescription("this is a discription");
    data.setSpecialPermissionSet(true);
    data.setProjectAdminUser("someadmin");

    spyAdapter.setRestClient(restClient);

    BitbucketProjectData expected = new BitbucketProjectData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);
    Mockito.doNothing()
        .when(spyAdapter)
        .setProjectPermissions(
            any(), any(), any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    doReturn(uri).when(spyAdapter).getAdapterApiUri();

    InOrder order = inOrder(spyAdapter);

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    // once for each group
    order
        .verify(spyAdapter)
        .setProjectPermissions(
            eq(expected),
            eq("groups"),
            any(),
            eq(BitbucketAdapter.PROJECT_PERMISSIONS.PROJECT_READ));
    order
        .verify(spyAdapter)
        .setProjectPermissions(
            eq(expected),
            eq("groups"),
            any(),
            eq(BitbucketAdapter.PROJECT_PERMISSIONS.PROJECT_WRITE));
    order
        .verify(spyAdapter, Mockito.times(2))
        .setProjectPermissions(
            eq(expected),
            eq("groups"),
            any(),
            eq(BitbucketAdapter.PROJECT_PERMISSIONS.PROJECT_ADMIN));
    // one for the tech user!
    verify(spyAdapter, Mockito.times(1))
        .setProjectPermissions(
            eq(expected),
            eq("users"),
            eq("cd_user"),
            any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }

  @Test
  public void callCreateProjectApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    OpenProjectData data = new OpenProjectData();
    data.setProjectKey("testkey");
    data.setProjectName("testproject");
    data.setDescription("this is a discription");
    data.setSpecialPermissionSet(false);

    BitbucketProjectData expected = new BitbucketProjectData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);
    Mockito.doNothing()
        .when(spyAdapter)
        .setProjectPermissions(
            any(), any(), any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    doReturn(uri).when(spyAdapter).getAdapterApiUri();

    InOrder order = inOrder(spyAdapter);

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    // only for the keyuser Group
    order
        .verify(spyAdapter)
        .setProjectPermissions(
            eq(expected),
            eq("groups"),
            any(),
            eq(BitbucketAdapter.PROJECT_PERMISSIONS.PROJECT_READ));
    order
        .verify(spyAdapter)
        .setProjectPermissions(
            eq(expected),
            eq("groups"),
            any(),
            eq(BitbucketAdapter.PROJECT_PERMISSIONS.PROJECT_WRITE));

    // one for the tech user!
    verify(spyAdapter, Mockito.times(1))
        .setProjectPermissions(
            eq(expected),
            eq("users"),
            eq("cd_user"),
            any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }

  @Test
  public void callCreateRepoApiWithoutAdminGroupTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    repo.setAdminGroup("");
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";

    RepositoryData expected = new RepositoryData();
    RepositoryData actual;

    doReturn(basePath).when(spyAdapter).getAdapterApiUri();

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);

    actual = spyAdapter.callCreateRepoApi(projectKey, false, repo);

    verify(spyAdapter, never()).setRepositoryAdminPermissions(any(), any(), eq("groups"), any());

    verify(spyAdapter)
        .setRepositoryWritePermissions(eq(expected), eq(projectKey), eq("users"), any());

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    assertEquals(expected, actual);
  }

  @Test
  public void callCreateRepoApiWithAdminGroupTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    repo.setAdminGroup("admins");
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";

    RepositoryData expected = new RepositoryData();
    RepositoryData actual;

    doReturn(basePath).when(spyAdapter).getAdapterApiUri();

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);

    actual = spyAdapter.callCreateRepoApi(projectKey, false, repo);

    verify(spyAdapter)
        .setRepositoryAdminPermissions(eq(expected), eq(projectKey), eq("groups"), eq("admins"));

    verify(spyAdapter)
        .setRepositoryWritePermissions(eq(expected), eq(projectKey), eq("users"), any());

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    assertEquals(expected, actual);
  }

  @Test
  public void callCreateRepoApiWithSpecialPermissionSetTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    repo.setAdminGroup("admins");
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";

    RepositoryData expected = new RepositoryData();
    RepositoryData actual;

    doReturn(basePath).when(spyAdapter).getAdapterApiUri();

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);

    actual = spyAdapter.callCreateRepoApi(projectKey, true, repo);

    verify(spyAdapter, never()).setRepositoryAdminPermissions(any(), any(), eq("groups"), any());

    verify(spyAdapter)
        .setRepositoryWritePermissions(eq(expected), eq(projectKey), eq("users"), any());

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    assertEquals(expected, actual);
  }

  @Test
  public void createAuxiliaryRepositoriesForProjectTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    OpenProjectData projectData = new OpenProjectData();
    projectData.setRepositories(new HashMap<>());
    projectData.setProjectKey("12423qtr");
    String crowdCookieValue = "cookieValue";
    String[] auxRepos = new String[] {"auxrepo1", "auxrepo2"};

    Repository repo1 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.getProjectKey().toLowerCase(), "auxrepo1"));

    Repository repo2 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.getProjectKey().toLowerCase(), "auxrepo2"));

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    doReturn(repoData1).when(spyAdapter).callCreateRepoApi(any(), anyBoolean(), any());

    spyAdapter.createAuxiliaryRepositoriesForODSProject(projectData, auxRepos);
    verify(spyAdapter, times(2))
        .callCreateRepoApi(
            eq(projectData.getProjectKey()),
            eq(false),
            argThat(
                repo ->
                    TEST_DEFAULT_ADMIN_GROUP.equals(repo.getAdminGroup())
                        && TEST_DEFAULT_USER_GROUP.equals(repo.getUserGroup())));
    Map<String, Map<URL_TYPE, String>> actual;
    actual = projectData.getRepositories();

    assertEquals(repoData1.convertRepoToOpenDataProjectRepo(), actual);
  }

  @Test
  public void createAuxiliaryRepositoriesForProjectWithSpecialPermissionSetTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    OpenProjectData projectData = new OpenProjectData();
    projectData.setRepositories(new HashMap<>());
    projectData.setProjectKey("12423qtr");
    projectData.setSpecialPermissionSet(true);
    projectData.setProjectAdminGroup("projectAdminGroup");
    projectData.setProjectUserGroup("projectUserGroup");
    String crowdCookieValue = "cookieValue";
    String[] auxRepos = new String[] {"auxrepo1", "auxrepo2"};

    Repository repo1 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.getProjectKey().toLowerCase(), "auxrepo1"));

    Repository repo2 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.getProjectKey().toLowerCase(), "auxrepo2"));

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    doReturn(repoData1).when(spyAdapter).callCreateRepoApi(any(), anyBoolean(), any());

    spyAdapter.createAuxiliaryRepositoriesForODSProject(projectData, auxRepos);
    verify(spyAdapter, times(2))
        .callCreateRepoApi(
            eq(projectData.getProjectKey()),
            eq(true),
            argThat(
                repo ->
                    projectData.getProjectAdminGroup().equals(repo.getAdminGroup())
                        && projectData.getProjectUserGroup().equals(repo.getUserGroup())));
    Map<String, Map<URL_TYPE, String>> actual;
    actual = projectData.getRepositories();

    assertEquals(repoData1.convertRepoToOpenDataProjectRepo(), actual);
  }

  @Test
  public void testCreateWebhooks() throws Exception {
    OpenProjectData projectData = new OpenProjectData();
    projectData.setRepositories(new HashMap<>());
    projectData.setProjectKey("12423qtr");

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    ValueCaptor<Webhook> bodyCaptor = new ValueCaptor<>();

    mockExecute(matchesClientCall().method(HttpMethod.POST).bodyCaptor(bodyCaptor))
        .thenReturn(repoData1);

    spyAdapter.createWebHooksForRepository(repoData1, projectData, "testComponent");
    // Verify that configured webhook events are added to the webhook request
    assertEquals(webhookEvents, bodyCaptor.getValues().get(0).getEvents());
  }

  @Test
  public void testCreateNoWebhookForReleaseManager() {

    OpenProjectData projectData = new OpenProjectData();
    projectData.setProjectKey("PWRM");
    projectData.setProjectKey("12423qtr");

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    spyAdapter.createWebHooksForRepository(repoData1, projectData, "release-manager");

    verify(spyAdapter, never()).getAdapterApiUri();
  }

  private Map<String, List<Link>> generateRepoLinks(String[] linkNames) {
    var linkNameList =
        Arrays.stream(linkNames)
            .map(
                linkname -> {
                  var link = new Link();
                  link.setName(linkname);
                  return link;
                })
            .collect(Collectors.toList());
    return Map.of("links", linkNameList);
  }

  private OpenProjectData getReturnOpenProjectData() {
    OpenProjectData data = new OpenProjectData();
    data.setQuickstarters(getReturnQuickstarters());
    data.setProjectKey("testkey");
    return data;
  }

  private List<Map<String, String>> getReturnQuickstarters() {
    Map<String, String> quickstart = new HashMap<>();
    quickstart.put(COMPONENT_ID_KEY, "testid");
    quickstart.put(COMPONENT_TYPE_KEY, "testComponent");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);
    return quickstarters;
  }

  private BitbucketProjectData getReturnBitbucketData() {
    return new BitbucketProjectData();
  }

  private RepositoryData getReturnRepoData() {
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    repoData.setName("testRepo");
    return repoData;
  }

  private Map<String, List<Link>> getReturnLinks() {
    Map<String, List<Link>> links = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("http");
    link.setHref("clone");
    linkList.add(link);
    links.put("clone", linkList);
    return links;
  }

  @Test
  public void testUserExistsCheck() throws IOException {

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    String response = fileReader.readFileContent("bitbucket-get-admin-user-response");

    OpenProjectData project = new OpenProjectData();

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkUser =
        spyAdapter.createCheckUser(project);
    assertNotNull(checkUser);

    List<CheckPreconditionFailure> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkUser.apply(result);
      fail();
    } catch (Exception ex) {
      assertTrue(ex instanceof IllegalArgumentException);
    }

    // Rest API return http error 404
    HttpException notFound = new HttpException(404, "not found");
    when(restClient.execute(isNotNull())).thenThrow(notFound);
    List<CheckPreconditionFailure> failures = checkUser.apply(result);
    assertTrue(failures.get(0).toString().contains(spyAdapter.getTechnicalUser()));

    // Rest API return other http error than 404
    try {
      when(restClient.execute(isNotNull()))
          .thenThrow(new HttpException(500, "internal server error"));
      checkUser.apply(result);
      fail();
    } catch (AdapterException e) {
      assertTrue(e.getCause() instanceof HttpException);
    }

    // Case no error, user exists!
    result = new ArrayList<>();
    when(restClient.execute(isNotNull())).thenReturn(response);
    List<CheckPreconditionFailure> newResult = checkUser.apply(result);
    assertEquals(0, newResult.size());

    // Case error, user does not exists!
    project.setCdUser("this_cd_user_not_exist".toUpperCase());
    checkUser = spyAdapter.createCheckUser(project);
    newResult = checkUser.apply(result);
    assertEquals(1, newResult.size());
    assertTrue(newResult.get(0).toString().contains(project.getCdUser()));
  }

  @Test
  public void testProjectExistsCheck() throws IOException {

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("TESTP");

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkProjectExists =
        spyAdapter.createProjectKeyExistsCheck(project.getProjectKey());
    assertNotNull(checkProjectExists);

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenReturn(null);
      checkProjectExists.apply(new ArrayList<>());
      fail();
    } catch (Exception ex) {
      assertTrue(ex instanceof IllegalArgumentException);
    }

    // Rest API return other http error than 404
    try {
      when(restClient.execute(isNotNull(), anyBoolean()))
          .thenThrow(new HttpException(500, "internal server error"));
      checkProjectExists.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      assertTrue(e.getCause() instanceof HttpException);
    }

    // Case error, project exists!
    String response = fileReader.readFileContent("bitbucket-get-project");
    when(restClient.execute(isNotNull(), anyBoolean())).thenReturn(response);
    List<CheckPreconditionFailure> failures = checkProjectExists.apply(new ArrayList<>());
    assertTrue(failures.get(0).toString().contains(project.getProjectKey()));

    // Rest API return http error 404
    HttpException notFound = new HttpException(404, "not found");
    when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(notFound);
    failures = checkProjectExists.apply(new ArrayList<>());
    assertEquals(0, failures.size());
  }

  @Test
  public void whenHandleExceptionWithinCheckCreateProjectPreconditionsThenException()
      throws IOException {

    bitbucketAdapter.setRestClient(restClient);
    bitbucketAdapter.setUseTechnicalUser(true);
    bitbucketAdapter.setUserName(TEST_USER_NAME);
    bitbucketAdapter.setUserPassword(TEST_USER_PASSWORD);

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("PKEY");

    IOException ioException = new IOException("throw in unit test");

    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      assertTrue(e.getMessage().contains(BitbucketAdapter.ADAPTER_NAME));
      assertTrue(e.getMessage().contains(project.getProjectKey()));
    }

    NullPointerException npe = new NullPointerException("npe throw in unit test");
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(npe);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getMessage().contains("Unexpected error"));
      assertTrue(e.getMessage().contains(project.getProjectKey()));
    }
  }
}
