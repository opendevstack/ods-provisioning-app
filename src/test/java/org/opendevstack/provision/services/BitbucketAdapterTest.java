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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.BitbucketData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.RepositoryData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class BitbucketAdapterTest {

  @Mock
  BitbucketData bitbucketData;
  @Mock
  BitbucketProject project;
  
  @InjectMocks
  @Autowired
  BitbucketAdapter bitbucketAdapter;
  @Mock
  Repository repo;
  String crowdCookieValue;

  @Test
  public void createBitbucketProjectsForProject() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    Map<String, List<Link>> map = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("testname");
    link.setHref("testlink");
    linkList.add(link);
    map.put("self", linkList);
    BitbucketData bitbucketData = getReturnBitbucketData();
    bitbucketData.setLinks(map);
    
    Mockito.doReturn(bitbucketData).when(spyAdapter)
        .callCreateProjectApi(Matchers.any(ProjectData.class), Matchers.anyString());

    ProjectData data =
        spyAdapter.createBitbucketProjectsForProject(getReturnProjectData(), crowdCookieValue);

    assertEquals("testlink", data.bitbucketUrl);
  }

  @Test
  public void createRepositoriesForProject() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    
    SecurityContextHolder.setContext(securityContext);
    ProjectData projectData = getReturnProjectData();    
    RepositoryData repoData = getReturnRepoData();

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any(), Matchers.anyString());
    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(Matchers.anyString(),
        Matchers.any(Repository.class), Matchers.anyString());

    ProjectData result = spyAdapter.createRepositoriesForProject(projectData, crowdCookieValue);

    Mockito.verify(spyAdapter).createWebHooksForRepository(repoData, projectData,
        projectData.quickstart.get(0).get("component_id"), crowdCookieValue, null);
    for (Entry<String, Map<String, List<Link>>> entry : result.repositories.entrySet()) {
      Map<String, List<Link>> resultLinkMap = entry.getValue();
      assertEquals(repoData.getLinks(), resultLinkMap);
    }
  }

  @Test
  public void createRepositoriesForProjectWhenRepositoriesNotEqNull() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    CrowdUserDetails principal = Mockito.mock(CrowdUserDetails.class);
    Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Repository repo = new Repository();
    ProjectData projectData = getReturnProjectData();
    Map<String, Map<String, List<Link>>> repos = new HashMap();
    projectData.repositories = repos;

    Map<String, String> quickstart = new HashMap<>();
    quickstart.put("component_id", "testid");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);

    projectData.quickstart = quickstarters;
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    projectData.key = "testkey";

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(repoData, projectData,
        quickstart.get("component_id"), crowdCookieValue, null);
    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(Matchers.anyString(),
        Matchers.any(Repository.class), Matchers.any());

    ProjectData result = spyAdapter.createRepositoriesForProject(projectData, crowdCookieValue);

    for (Entry<String, Map<String, List<Link>>> entry : result.repositories.entrySet()) {
      Map<String, List<Link>> resultLinkMap = entry.getValue();
      assertEquals(repoData.getLinks(), resultLinkMap);
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

    ProjectData projectData = new ProjectData();

    RepositoryData repoData = new RepositoryData();

    Map<String, List<Link>> links = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("testname");
    link.setHref("clone");
    linkList.add(link);
    links.put("clone", linkList);
    repoData.setLinks(links);

    Mockito.doNothing().when(spyAdapter).createWebHooksForRepository(Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any(), Matchers.anyString());
    Mockito.doReturn(repoData).when(spyAdapter).callCreateRepoApi(Matchers.anyString(),
        Matchers.any(Repository.class), Matchers.anyString());

    ProjectData actual = spyAdapter.createRepositoriesForProject(projectData, crowdCookieValue);

    assertEquals(projectData, actual);
  }

  @Test
  public void callCreateProjectApiWithPermissionSetTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    ProjectData data = new ProjectData();
    data.key = "testkey";
    data.name = "testproject";
    data.description = "this is a discription";
    data.createpermissionset = true;
    data.admin = "someadmin";

    BitbucketProject project = BitbucketAdapter.createBitbucketProject(data);

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(project);


    BitbucketData expected = new BitbucketData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    Mockito.doReturn(expected).when(spyAdapter).post(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any());
    Mockito.doNothing().when(spyAdapter).setProjectPermissions(Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any(), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    Mockito.doReturn(uri).when(spyAdapter).buildBasePath();

    BitbucketData actual = spyAdapter.callCreateProjectApi(data, crowdCookieValue);

    Mockito.verify(spyAdapter).post(Matchers.eq(uri), Matchers.eq(json),
        Matchers.eq(crowdCookieValue), Matchers.any());
    // once for each group
    Mockito.verify(spyAdapter, Mockito.times(4)).setProjectPermissions(Matchers.eq(expected), Matchers.eq("groups"),
        Matchers.any(), Matchers.eq(crowdCookieValue), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    // one for the tech user!
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(Matchers.eq(expected), Matchers.eq("users"),
    	Matchers.eq("cd_user"), Matchers.eq(crowdCookieValue), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }

  @Test
  public void callCreateProjectApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    String uri = "http://192.168.56.31:7990/rest/api/1.0/projects";

    ProjectData data = new ProjectData();
    data.key = "testkey";
    data.name = "testproject";
    data.description = "this is a discription";
    data.createpermissionset = false;

    BitbucketProject project = BitbucketAdapter.createBitbucketProject(data);

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(project);

    BitbucketData expected = new BitbucketData();
    expected.setDescription("this is a discription");
    expected.setName("testproject");
    expected.setKey("testkey");
    expected.setId("13231");

    Mockito.doReturn(expected).when(spyAdapter).post(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any());
    Mockito.doNothing().when(spyAdapter).setProjectPermissions(Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any(), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    Mockito.doReturn(uri).when(spyAdapter).buildBasePath();

    BitbucketData actual = spyAdapter.callCreateProjectApi(data, crowdCookieValue);

    Mockito.verify(spyAdapter).post(Matchers.eq(uri), Matchers.eq(json),
        Matchers.eq(crowdCookieValue), Matchers.any());
    // only for the keyuser Group
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(Matchers.eq(expected), Matchers.eq("groups"),
        Matchers.any(), Matchers.eq(crowdCookieValue), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));
    // one for the tech user!
    Mockito.verify(spyAdapter, Mockito.times(1)).setProjectPermissions(Matchers.eq(expected), Matchers.eq("users"),
    	Matchers.eq("cd_user"), Matchers.eq(crowdCookieValue), Matchers.any(BitbucketAdapter.PROJECT_PERMISSIONS.class));

    assertEquals(expected, actual);
  }
  
  @Test
  public void callCreateRepoApiTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    Repository repo = new Repository();
    repo.setName("testrepo");
    repo.setScmId("testscmid");
    repo.setForkable(true);
    String projectKey = "testkey";
    String basePath = "http://192.168.56.31:7990/rest/api/1.0";
    String uri = "http://192.168.56.31:7990/rest/api/1.0/testkey/repos";


    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(repo);

    RepositoryData expected = new RepositoryData();

    Mockito.doReturn(basePath).when(spyAdapter).buildBasePath();
    Mockito.doReturn(expected).when(spyAdapter).post(Matchers.anyString(), Matchers.any(),
        Matchers.same(crowdCookieValue), Matchers.any());
    Mockito.doNothing().when(spyAdapter).setRepositoryPermissions(Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any(), Matchers.any());

    RepositoryData actual = spyAdapter.callCreateRepoApi(projectKey, repo, crowdCookieValue);

    Mockito.verify(spyAdapter).setRepositoryPermissions(Matchers.eq(expected),
        Matchers.eq(projectKey), Matchers.eq("groups"), Matchers.any(),
        Matchers.eq(crowdCookieValue));
    Mockito.verify(spyAdapter).setRepositoryPermissions(Matchers.eq(expected),
        Matchers.eq(projectKey), Matchers.eq("users"), Matchers.any(),
        Matchers.eq(crowdCookieValue));
    Mockito.verify(spyAdapter).post(Matchers.eq(uri), Matchers.eq(json),
        Matchers.same(crowdCookieValue), Matchers.any());


    assertEquals(expected, actual);
  }

  @Test
  public void createAuxiliaryRepositoriesForProjectTest() throws Exception {
    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

    ProjectData projectData = new ProjectData();
    projectData.repositories = new HashMap<>();
    projectData.key = "12423qtr";
    String crowdCookieValue = "cookieValue";
    String[] auxRepos = new String[] {"auxrepo1", "auxrepo2"};

    Repository repo1 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.key.toLowerCase(), "auxrepo1"));

    Repository repo2 = new Repository();
    repo1.setName(String.format("%s-%s", projectData.key.toLowerCase(), "auxrepo2"));


    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    RepositoryData repoData2 = new RepositoryData();
    repoData2.setName("repoData2");
    repoData2.setLinks(generateRepoLinks(new String[] {"link3", "link4"}));

    Map<String, Map<String, List<Link>>> expected = new HashMap<>();
    expected.put("repoData1", generateRepoLinks(new String[] {"link1", "link2"}));
    // expected.put("repoData2", generateRepoLinks(new String[]{"link3", "link4"}));

    Mockito.doReturn(repoData1).when(spyAdapter).callCreateRepoApi(Matchers.any(), Matchers.any(),
        Matchers.any());
    // Mockito.doReturn(repoData2).when(spyAdapter).callCreateRepoApi(Matchers.any(),
    // Matchers.any(), Matchers.any());

    spyAdapter.createAuxiliaryRepositoriesForProject(projectData, crowdCookieValue, auxRepos);
    Map<String, Map<String, List<Link>>> actual;
    actual = projectData.repositories;

    assertEquals(expected, actual);
  }

  @Test
  public void testCreateWebhooks() throws Exception {

    ProjectData projectData = new ProjectData();
    projectData.repositories = new HashMap<>();
    projectData.key = "12423qtr";

    RepositoryData repoData1 = new RepositoryData();
    repoData1.setName("repoData1");
    repoData1.setLinks(generateRepoLinks(new String[] {"link1", "link2"}));

    BitbucketAdapter spyAdapter = Mockito.spy(bitbucketAdapter);

	Mockito.doReturn(repoData1).when(spyAdapter).post(Matchers.anyString(), Matchers.anyString(), 
			Matchers.anyString(), Matchers.any());
    
    spyAdapter.createWebHooksForRepository
    	(repoData1, projectData, "someComponent", "crowdCookie", "componentType");
    
    
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

  private ProjectData getReturnProjectData() {
    ProjectData data = new ProjectData();
    data.quickstart = getReturnQuickstarters();
    data.key = "testkey";
    return data;
  }

  private List<Map<String, String>> getReturnQuickstarters() {
    Map<String, String> quickstart = new HashMap<>();
    quickstart.put("component_id", "testid");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(quickstart);
    return quickstarters;
  }

  private BitbucketData getReturnBitbucketData() {
    BitbucketData data = new BitbucketData();
    return data;
  }

  private BitbucketProject getTestProject() {
    BitbucketProject project = new BitbucketProject();
    return project;
  }
  
  private RepositoryData getReturnRepoData() {
    RepositoryData repoData = new RepositoryData();
    repoData.setLinks(getReturnLinks());
    return repoData;
  }

  private Map<String, List<Link>> getReturnLinks() {
    Map<String, List<Link>> links = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    Link link = new Link();
    link.setName("testname");
    link.setHref("clone");
    linkList.add(link);
    links.put("clone", linkList);
    return links;
  }
}
