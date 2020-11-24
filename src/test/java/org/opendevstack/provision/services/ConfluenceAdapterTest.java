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

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.model.confluence.SpaceData;
import org.opendevstack.provision.util.TestDataFileReader;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@ActiveProfiles("utest")
@DirtiesContext
public class ConfluenceAdapterTest extends AbstractBaseServiceAdapterTest {

  private static final Logger logger = LoggerFactory.getLogger(ConfluenceAdapterTest.class);

  public static final String TEST_USER_NAME = "testUserName";
  public static final String TEST_USER_PASSWORD = "testUserPassword";

  private ObjectMapper objectMapper;

  private static TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  @MockBean private IODSAuthnzAdapter authnzAdapter;

  @Autowired @InjectMocks ConfluenceAdapter confluenceAdapter;

  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;

  @Autowired ConfigurableEnvironment environment;

  @Before
  public void initTests() {
    when(authnzAdapter.getUserName()).thenReturn(TEST_USER_NAME);
    when(authnzAdapter.getUserPassword()).thenReturn(TEST_USER_PASSWORD);
  }

  @Test
  public void createConfluenceSpaceForProject() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    OpenProjectData project = Mockito.mock(OpenProjectData.class);
    SpaceData spaceData = Mockito.mock(SpaceData.class);
    Space space = Mockito.mock(Space.class);

    doReturn(space).when(spyAdapter).createSpaceData(project);
    doReturn(spaceData).when(spyAdapter).callCreateSpaceApi(any(Space.class));
    when(spaceData.getUrl()).thenReturn("testUrl");

    String createdProjectString = spyAdapter.createCollaborationSpaceForODSProject(project);

    assertEquals("testUrl", createdProjectString);
  }

  @Test
  public void callCreateSpaceApi() throws Exception {
    Space space = new Space();
    SpaceData expectedSpaceData = Mockito.mock(SpaceData.class);

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(expectedSpaceData);
    SpaceData createdSpaceData = confluenceAdapter.callCreateSpaceApi(space);

    assertEquals(expectedSpaceData, createdSpaceData);
  }

  @Test
  public void updateSpacePermissions() throws Exception {
    OpenProjectData project = JiraAdapterTests.getTestProject("name");
    project.projectAdminGroup = "adminGroup";
    project.projectUserGroup = "userGroup";
    project.projectReadonlyGroup = "readGroup";

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(String.class);
    int permissionSets = confluenceAdapter.updateSpacePermissions(project);

    // 3 permission sets
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.projectAdminGroup))));
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.projectUserGroup))));
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.projectReadonlyGroup))));
    assertEquals(3, permissionSets);
  }

  @Test
  public void testCreateSpaceData() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    OpenProjectData project = JiraAdapterTests.getTestProject("name");
    project.projectAdminGroup = "adminGroup";
    project.projectUserGroup = "adminGroup";
    project.projectReadonlyGroup = "adminGroup";

    List blList = new ArrayList<>();
    Blueprint bPrint = new Blueprint();
    bPrint.setBlueprintModuleCompleteKey(confluenceBlueprintKey);
    bPrint.setContentBlueprintId("1234");
    blList.add(bPrint);

    Mockito.doReturn(blList)
        .when(spyAdapter)
        .getSpaceTemplateList(contains("space-blueprint"), any());

    Mockito.doReturn(new ArrayList<>())
        .when(spyAdapter)
        .getSpaceTemplateList(contains("jira"), any());

    Space space = spyAdapter.createSpaceData(project);

    assertNotNull(space);
    assertEquals(project.projectName, space.getName());
    assertEquals(project.projectKey, space.getSpaceKey());

    assertNotNull(space.getContext());

    assertEquals(project.projectName, space.getContext().getProjectName());
    assertEquals(project.projectKey, space.getContext().getProjectKey());
  }

  @Test
  public void testTemplateKeyLookup() {
    String defaultTemplateName =
        "com.atlassian.confluence.plugins.confluence-space-blueprints:documentation-space-blueprint";

    OpenProjectData project = new OpenProjectData();
    Map<IServiceAdapter.PROJECT_TEMPLATE, String> templates =
        confluenceAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    assertEquals(1, templates.size());
    assertEquals(defaultTemplateName, templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

    project.projectType = "notExistant";
    templates = confluenceAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    assertEquals(1, templates.size());
    assertEquals(defaultTemplateName, templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

    // add new template
    confluenceAdapter
        .environment
        .getSystemProperties()
        .put("confluence.blueprint.key.testTemplate", "template");

    confluenceAdapter.projectTemplateKeyNames.add("testTemplate");

    project.projectType = "testTemplate";

    templates = confluenceAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    assertEquals(1, templates.size());
    assertEquals("template", templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));
  }

  @Test
  public void testAdapterURI() {
    assertEquals("http://192.168.56.31:8090/rest", confluenceAdapter.getAdapterApiUri());
  }

  @Test
  public void whenHandleExceptionWithinCheckCreateProjectPreconditionsThenException()
      throws IOException {

    confluenceAdapter.setRestClient(restClient);
    confluenceAdapter.useTechnicalUser = true;
    confluenceAdapter.userName = TEST_USER_NAME;
    confluenceAdapter.userPassword = TEST_USER_PASSWORD;

    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);

    OpenProjectData project = new OpenProjectData();
    project.projectKey = "PKEY";

    IOException ioException = new IOException("throw in unit test");
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      Assert.assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      Assert.assertTrue(e.getMessage().contains(ConfluenceAdapter.ADAPTER_NAME));
      Assert.assertTrue(e.getMessage().contains(project.projectKey));
    }

    NullPointerException npe = new NullPointerException("npe throw in unit test");
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(npe);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      Assert.assertTrue(e.getMessage().contains("Unexpected error"));
      Assert.assertTrue(e.getMessage().contains(project.projectKey));
    }
  }

  @Test
  public void testProjectKeyExistCheck() throws IOException {

    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    spyAdapter.setRestClient(restClient);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("TESTP");

    Function<List<CheckPreconditionFailure>, List<CheckPreconditionFailure>> checkProjectKeyExists =
        spyAdapter.createProjectKeyExistsCheck(project.getProjectKey());
    assertNotNull(checkProjectKeyExists);

    // Case one, an exception happens
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenReturn(null);
      checkProjectKeyExists.apply(new ArrayList<>());
      fail();
    } catch (Exception e) {
      Assert.assertTrue(IllegalArgumentException.class.isInstance(e));
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(ioException);
      checkProjectKeyExists.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      Assert.assertEquals(ioException, e.getCause());
    }

    // Case error, project key exists!
    String response =
        fileReader
            .readFileContent("confluence-get-space-template")
            .replace("<%SPACE%>", project.getProjectKey().toUpperCase());

    when(restClient.execute(isNotNull(), anyBoolean())).thenReturn(response);
    List<CheckPreconditionFailure> newResult = checkProjectKeyExists.apply(new ArrayList<>());
    Assert.assertEquals(1, newResult.size());
    Assert.assertTrue(
        newResult
            .get(0)
            .toString()
            .contains(CheckPreconditionFailure.ExceptionCodes.PROJECT_EXISTS.toString()));

    // Case project key does not exists -> all ok
    String thisProjectKeyNotExists = "UNEXISTANT_PROJECT_KEY".toUpperCase();
    HttpException notFound = new HttpException(404, "not found");
    when(restClient.execute(isNotNull())).thenThrow(notFound);
    checkProjectKeyExists = spyAdapter.createProjectKeyExistsCheck(thisProjectKeyNotExists);
    newResult = checkProjectKeyExists.apply(new ArrayList<>());
    Assert.assertEquals(0, newResult.size());
  }
}
