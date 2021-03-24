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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("utest")
@DirtiesContext
public class ConfluenceAdapterTest extends AbstractBaseServiceAdapterTest {

  public static final String TEST_USER_NAME = "testUserName";
  public static final String TEST_USER_PASSWORD = "testUserPassword";

  private static final TestDataFileReader fileReader =
      new TestDataFileReader(TestDataFileReader.TEST_DATA_FILE_DIR);

  @Autowired private ConfluenceAdapter confluenceAdapter;

  @MockBean private IODSAuthnzAdapter authnzAdapter;

  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;

  @BeforeEach
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
    project.setProjectAdminGroup("adminGroup");
    project.setProjectUserGroup("userGroup");
    project.setProjectReadonlyGroup("readGroup");

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(String.class);
    int permissionSets = confluenceAdapter.updateSpacePermissions(project);

    // 3 permission sets
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.getProjectAdminGroup()))));
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.getProjectUserGroup()))));
    verifyExecute(
        matchesClientCall()
            .method(HttpMethod.POST)
            .bodyMatches(hasToString(containsString(project.getProjectReadonlyGroup()))));
    assertEquals(3, permissionSets);
  }

  @Test
  public void testCreateSpaceData() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    OpenProjectData project = JiraAdapterTests.getTestProject("name");
    project.setProjectAdminGroup("adminGroup");
    project.setProjectUserGroup("adminGroup");
    project.setProjectReadonlyGroup("adminGroup");

    var blList = new ArrayList<>();
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
    assertEquals(project.getProjectName(), space.getName());
    assertEquals(project.getProjectKey(), space.getSpaceKey());

    assertNotNull(space.getContext());

    assertEquals(project.getProjectName(), space.getContext().getProjectName());
    assertEquals(project.getProjectKey(), space.getContext().getProjectKey());
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

    project.setProjectType("notExistant");
    templates = confluenceAdapter.retrieveInternalProjectTypeAndTemplateFromProjectType(project);

    assertEquals(1, templates.size());
    assertEquals(defaultTemplateName, templates.get(IServiceAdapter.PROJECT_TEMPLATE.TEMPLATE_KEY));

    // add new template
    confluenceAdapter
        .getEnvironment()
        .getSystemProperties()
        .put("confluence.blueprint.key.testTemplate", "template");

    confluenceAdapter.getProjectTemplateKeyNames().add("testTemplate");

    project.setProjectType("testTemplate");

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
    confluenceAdapter.setUseTechnicalUser(true);
    confluenceAdapter.setUserName(TEST_USER_NAME);
    confluenceAdapter.setUserPassword(TEST_USER_PASSWORD);

    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);

    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("PKEY");

    IOException ioException = new IOException("throw in unit test");
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(ioException);

      spyAdapter.checkCreateProjectPreconditions(project);
      fail();

    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getCause().getCause().getMessage().contains(ioException.getMessage()));
      assertTrue(e.getMessage().contains(ConfluenceAdapter.ADAPTER_NAME));
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
      assertTrue(e instanceof IllegalArgumentException);
    }

    // Case IOException throw from rest client!
    IOException ioException = new IOException();
    try {
      when(restClient.execute(isNotNull(), anyBoolean())).thenThrow(ioException);
      checkProjectKeyExists.apply(new ArrayList<>());
      fail();
    } catch (AdapterException e) {
      assertEquals(ioException, e.getCause());
    }

    // Case error, project key exists!
    String response =
        fileReader
            .readFileContent("confluence-get-space-template")
            .replace("<%SPACE%>", project.getProjectKey().toUpperCase());

    when(restClient.execute(isNotNull(), anyBoolean())).thenReturn(response);
    List<CheckPreconditionFailure> newResult = checkProjectKeyExists.apply(new ArrayList<>());
    assertEquals(1, newResult.size());
    assertTrue(
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
    assertEquals(0, newResult.size());
  }
}
