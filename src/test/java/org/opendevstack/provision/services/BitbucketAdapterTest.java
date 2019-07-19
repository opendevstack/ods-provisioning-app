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

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.rest.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class BitbucketAdapterTest {

  @Mock
  BitbucketProjectData bitbucketData;
  @Mock
  BitbucketProject project;

  @Mock
  Client client;
  RestClient restClient;

  @InjectMocks
  @Autowired
  BitbucketAdapter bitbucketAdapter;
  @Mock
  Repository repo;
  String crowdCookieValue;

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

    Mockito.doReturn(bitbucketData).when(spyAdapter)
        .callCreateProjectApi(any(OpenProjectData.class));

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

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(any(), any());

    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(),
        any(Repository.class));

    Map<String, Map<URL_TYPE, String>> result =
        spyAdapter.createComponentRepositoriesForODSProject(projectData);

    Mockito.verify(spyAdapter).createWebHooksForRepository(repoData, projectData);
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

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(repoData, projectData);
    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(),
        any(Repository.class));

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

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(any(), any());
    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(anyString(),
        any(Repository.class));

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

    //TODO fix tests after migration org.opendevstack.provision.util.RestClient to org.opendevstack.provision.util.rest.Client
    spyAdapter.client = client;

    BitbucketProjectData expected = new BitbucketProjectData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    Mockito.doReturn(expected).when(restClient).callHttp(anyString(), any(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), any());

    Mockito.doNothing().when(spyAdapter).setProjectPermissions(any(), any(), any(),
        any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    Mockito.doReturn(uri).when(spyAdapter).getAdapterApiUri();

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    Mockito.verify(restClient).callHttp(eq(uri), isA(BitbucketProject.class), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), eq(BitbucketProjectData.class));

    // once for each group
    Mockito.verify(spyAdapter, Mockito.times(4)).setProjectPermissions(eq(expected), eq("groups"),
        any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    Mockito.verify(spyAdapter, Mockito.times(4)).setProjectPermissions(eq(expected), eq("groups"),
        any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    // one for the tech user!
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(eq(expected), eq("users"),
        eq("cd_user"), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }

  @Test
  public void callCreateProjectApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    //spyAdapter.client = client;

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    OpenProjectData data = new OpenProjectData();
    data.projectKey = "testkey";
    data.projectName = "testproject";
    data.description = "this is a discription";
    data.specialPermissionSet = false;

    BitbucketProject project = BitbucketAdapter.createBitbucketProject(data);

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

    BitbucketProjectData expected = new BitbucketProjectData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    Mockito.doReturn(expected).when(restClient).callHttp(anyString(), any(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), any());

    Mockito.doNothing().when(spyAdapter).setProjectPermissions(any(), any(), any(),
        any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    Mockito.doReturn(uri).when(spyAdapter).getAdapterApiUri();

    BitbucketProjectData actual = spyAdapter.callCreateProjectApi(data);

    Mockito.verify(restClient).callHttp(anyString(), any(), anyBoolean(), eq(RestClient.HTTP_VERB.POST),
        any());

    // only for the keyuser Group
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(eq(expected), eq("groups"),
        any(), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    // one for the tech user!
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(eq(expected), eq("users"),
        eq("cd_user"), any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }

  @Test
  public void callCreateRepoApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
//    spyAdapter.client = client;

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";
    String uri = "http://192.168.56.31:7990/rest/api/1.0/testkey/repos";

    RepositoryData expected = new RepositoryData();

    Mockito.doReturn(basePath).when(spyAdapter).getAdapterApiUri();

    Mockito.doReturn(expected).when(restClient).callHttp(anyString(), any(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), any());

    Mockito.doNothing().when(spyAdapter).setRepositoryPermissions(any(), any(), any(), any());

    RepositoryData actual = spyAdapter.callCreateRepoApi(projectKey, repo);

    Mockito.verify(spyAdapter).setRepositoryPermissions(eq(expected), eq(projectKey), eq("groups"),
        any());

    Mockito.verify(spyAdapter).setRepositoryPermissions(eq(expected), eq(projectKey), eq("users"),
        any());

    Mockito.verify(restClient).callHttp(eq(uri), eq(repo), anyBoolean(), eq(RestClient.HTTP_VERB.POST),
        any());

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

    Mockito.doReturn(repoData1).when(spyAdapter).callCreateRepoApi(any(), any());

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

    //spyAdapter.client = client;

    Mockito.doReturn(repoData1).when(restClient).callHttp(anyString(), anyString(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), any());

    spyAdapter.createWebHooksForRepository(repoData1, projectData);
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
    quickstart.put("component_id", "testid");
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
}
