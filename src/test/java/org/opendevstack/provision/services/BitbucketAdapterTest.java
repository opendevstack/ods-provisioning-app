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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_ID_KEY;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_TYPE_KEY;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Torsten Jaeschke */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@ActiveProfiles("utest")
public class BitbucketAdapterTest extends AbstractBaseServiceAdapterTest {

  private static final Logger logger = LoggerFactory.getLogger(BitbucketAdapterTest.class);

  public static final String TEST_USER_NAME = "testUserName";
  public static final String TEST_USER_PASSWORD = "testUserPassword";
  private static final int ONE_SEC_IN_MILLIS = 1000;

  @MockBean private IODSAuthnzAdapter authnzAdapter;

  @Mock BitbucketProjectData bitbucketData;
  @Mock BitbucketProject project;
  @InjectMocks @Autowired BitbucketAdapter bitbucketAdapter;
  @Mock Repository repo;

  private static TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  @Before
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

    doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(), any(Repository.class));

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

    Repository repo = new Repository();
    OpenProjectData projectData = getReturnOpenProjectData();
    Map<String, Map<URL_TYPE, String>> repos = new HashMap();
    projectData.repositories = repos;

    Map<String, String> quickstart = new HashMap<>();
    quickstart.put("component_id", "testid");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);

    projectData.quickstarters = quickstarters;
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    repoData.setName("testRepoName");
    projectData.projectKey = "testkey";

    Mockito.doNothing()
        .when(spyAdapter)
        .createWebHooksForRepository(repoData, projectData, "testComponent");
    doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> result =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    for (Entry<String, Map<URL_TYPE, String>> entry : result.entrySet()) {
      Map<URL_TYPE, String> resultLinkMap = entry.getValue();
      assertEquals(repoData.convertRepoToOpenDataProjectRepo(), resultLinkMap);
    }
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
    doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(), any(Repository.class));

    Map<String, Map<URL_TYPE, String>> actual =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    assertEquals(new HashMap<String, Map<URL_TYPE, String>>(), actual);
  }

  @Test
  public void callCreateProjectApiWithPermissionSetTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    OpenProjectData data = new OpenProjectData();
    data.projectKey = "testkey";
    data.projectName = "testproject";
    data.description = "this is a discription";
    data.specialPermissionSet = true;
    data.projectAdminUser = "someadmin";

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

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    // once for each group
    verify(spyAdapter, Mockito.times(4))
        .setProjectPermissions(
            eq(expected), eq("groups"), any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    verify(spyAdapter, Mockito.times(4))
        .setProjectPermissions(
            eq(expected), eq("groups"), any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
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
    data.projectKey = "testkey";
    data.projectName = "testproject";
    data.description = "this is a discription";
    data.specialPermissionSet = false;

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

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    // only for the keyuser Group
    verify(spyAdapter, Mockito.times(2))
        .setProjectPermissions(
            eq(expected), eq("groups"), any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

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
  public void callCreateRepoApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";
    String uri = "http://192.168.56.31:7990/rest/api/1.0/testkey/repos";

    RepositoryData expected = new RepositoryData();

    doReturn(basePath).when(spyAdapter).getAdapterApiUri();

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expected);

    Mockito.doNothing().when(spyAdapter).setRepositoryAdminPermissions(any(), any(), any(), any());

    RepositoryData actual = spyAdapter.callCreateRepoApi(projectKey, repo);

    verify(spyAdapter)
        .setRepositoryAdminPermissions(eq(expected), eq(projectKey), eq("groups"), any());

    verify(spyAdapter)
        .setRepositoryAdminPermissions(eq(expected), eq(projectKey), eq("users"), any());

    verifyExecute(matchesClientCall().method(HttpMethod.POST));
    assertEquals(expected, actual);
  }

  @Test
  public void createAuxiliaryRepositoriesForProjectTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    OpenProjectData projectData = new OpenProjectData();
    projectData.repositories = new HashMap<>();
    projectData.projectKey = "12423qtr";
    String crowdCookieValue = "cookieValue";
    String[] auxRepos = new String[] {"auxrepo1", "auxrepo2"};

    Repository repo1 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.projectKey.toLowerCase(), "auxrepo1"));

    Repository repo2 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.projectKey.toLowerCase(), "auxrepo2"));

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    doReturn(repoData1).when(spyAdapter).callCreateRepoApi(any(), any());

    spyAdapter.createAuxiliaryRepositoriesForODSProject(projectData, auxRepos);
    Map<String, Map<URL_TYPE, String>> actual;
    actual = projectData.repositories;

    assertEquals(repoData1.convertRepoToOpenDataProjectRepo(), actual);
  }

  @Test
  public void testCreateWebhooks() throws Exception {

    OpenProjectData projectData = new OpenProjectData();
    projectData.repositories = new HashMap<>();
    projectData.projectKey = "12423qtr";

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(repoData1);
    spyAdapter.createWebHooksForRepository(repoData1, projectData, "testComponent");
  }

  @Test
  public void testCreateNoWebhookForReleaseManager() throws Exception {

    OpenProjectData projectData = new OpenProjectData();
    projectData.projectKey = "PWRM";
    projectData.projectKey = "12423qtr";

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    spyAdapter.createWebHooksForRepository(repoData1, projectData, "release-manager");

    verify(spyAdapter, never()).getAdapterApiUri();
  }

  private Map<String, List<Link>> generateRepoLinks(String[] linknames) {
    List<Link> linkList = new ArrayList();
    for (String linkname : linknames) {
      Link link = new Link();
      link.setName(linkname);
      linkList.add(link);
    }
    Map<String, List<Link>> linkMap = new HashMap();
    linkMap.put("links", linkList);
    return linkMap;
  }

  private OpenProjectData getReturnOpenProjectData() {
    OpenProjectData data = new OpenProjectData();
    data.quickstarters = getReturnQuickstarters();
    data.projectKey = "testkey";
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
    BitbucketProjectData data = new BitbucketProjectData();
    return data;
  }

  private BitbucketProject getTestProject() {
    BitbucketProject project = new BitbucketProject();
    return project;
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
  public void testWhenCheckUser() throws IOException {

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    spyAdapter.setRestClient(restClient);

    String response = fileReader.readFileContent("bitbucket-get-admin-user-response");

    OpenProjectData project = new OpenProjectData();

    Function<List<String>, List<String>> checkUser = spyAdapter.createCheckUser(project);
    assertNotNull(checkUser);

    List<String> result = new ArrayList<>();

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull())).thenReturn(null);
      checkUser.apply(result);
      fail();
    } catch (Exception e) {
      assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Rest API return http error 404
    HttpException notFound = new HttpException(404, "not found");
    when(restClient.execute(isNotNull())).thenThrow(notFound);
    List<String> failures = checkUser.apply(result);
    assertTrue(failures.get(0).contains(spyAdapter.getTechnicalUser()));

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
    List<String> newResult = checkUser.apply(result);
    assertEquals(0, newResult.size());

    // Case error, user does not exists!
    project.cdUser = "this_cd_user_not_exist";
    checkUser = spyAdapter.createCheckUser(project);
    newResult = checkUser.apply(result);
    assertEquals(1, newResult.size());
    assertTrue(newResult.get(0).contains(project.cdUser));
  }

  @Test
  public void whenHandleExceptionWithinCheckCreateProjectPreconditionsThenException()
      throws IOException {

    bitbucketAdapter.setRestClient(restClient);
    bitbucketAdapter.useTechnicalUser = true;
    bitbucketAdapter.userName = TEST_USER_NAME;
    bitbucketAdapter.userPassword = TEST_USER_PASSWORD;

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    OpenProjectData project = new OpenProjectData();
    project.projectKey = "PKEY";

    IOException ioException = new IOException("throw in unit test");

    try {
      when(restClient.execute(isNotNull())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      assertTrue(e.getMessage().contains(BitbucketAdapter.ADAPTER_NAME));
      assertTrue(e.getMessage().contains(project.projectKey));
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
}
