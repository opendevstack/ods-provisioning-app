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
package org.opendevstack.provision.services.jira;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("utest")
public class JiraSpecialPermissionerTest {

  public static final Integer UTEST_CONFIGURED_PERMISSION_SCHEME_ID =
      99999; // value set in application-utest.properties
  public static final String UTEST_CONFIGURED_PROJECT_ROLE_ID =
      "55555"; // value set in application-utest.properties
  public static final String UTEST_PROJECT_TEMPLATE_NAME =
      "utest-project-template"; // value set in application-utest.properties

  @Value("${jira.api.path}")
  private String jiraApiPath;

  @Value("${jira.uri}")
  private String jiraUri;

  @Autowired private JiraSpecialPermissioner jiraSpecialPermissioner;

  private JiraRestService jiraRestService;
  private RestClient jiraClient;
  private RestClientCall restClientCall;

  public void setupMocks() {

    jiraRestService = mock(JiraRestService.class);
    jiraClient = mock(RestClient.class);
    restClientCall = mock(RestClientCall.class);
    when(jiraRestService.getRestClient()).thenReturn(jiraClient);
    when(jiraRestService.httpPost()).thenReturn(restClientCall);
    when(jiraRestService.httpPut()).thenReturn(restClientCall);
    when(restClientCall.url(anyString())).thenReturn(restClientCall);
    when(restClientCall.body(any())).thenReturn(restClientCall);
    when(restClientCall.header(any())).thenReturn(restClientCall);
    when(restClientCall.returnType(any())).thenReturn(restClientCall);
  }

  public void resetMocks() {}

  @Test
  public void setupProjectSpecialPermissions() throws IOException {

    String projectRole = "22222";

    // case project has a special permission id
    OpenProjectData projectWithPermissionSchemeIdAndProjectRoleMapping =
        createOpenProjectData("TEST1", projectRole, 10100, UTEST_PROJECT_TEMPLATE_NAME);

    OpenProjectData projectWithoutPermissionSchemeUsingConfiguration =
        createOpenProjectData("TEST2", null, null, UTEST_PROJECT_TEMPLATE_NAME);

    final Consumer<OpenProjectData> verifyPermissionScheme = createPermissionSchemeVerifier();

    Consumer<OpenProjectData> verifyProjectRole =
        openProjectData -> {
          String roleId =
              openProjectData.getSpecialPermissionSchemeId() != null
                  ? openProjectData.getProjectRoleForAdminGroup()
                  : UTEST_CONFIGURED_PROJECT_ROLE_ID;

          String postProjectRoleUrl =
              String.format(
                  JiraRestApi.JIRA_API_PROJECT_ROLE_PATTERN,
                  jiraUri,
                  jiraApiPath,
                  openProjectData.getProjectKey(),
                  roleId);

          verify(jiraRestService, times(3)).httpPost();
          verify(restClientCall, times(3)).url(contains(postProjectRoleUrl));
          verify(restClientCall, times(3)).body(any(ActorGroupPayload.class));
        };

    List.of(
            projectWithoutPermissionSchemeUsingConfiguration,
            projectWithPermissionSchemeIdAndProjectRoleMapping)
        .forEach(
            project -> {
              try {
                setupMocks();

                jiraSpecialPermissioner.setupProjectSpecialPermissions(jiraRestService, project);
                verifyPermissionScheme.accept(project);
                verifyProjectRole.accept(project);

                resetMocks();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @NotNull
  private Consumer<OpenProjectData> createPermissionSchemeVerifier() {
    final Consumer<OpenProjectData> verifyPermissionScheme =
        openProjectData -> {
          Integer schemeId =
              openProjectData.getSpecialPermissionSchemeId() != null
                  ? openProjectData.getSpecialPermissionSchemeId()
                  : UTEST_CONFIGURED_PERMISSION_SCHEME_ID;

          // Verify put permission scheme to project
          String putPermissionSchemeUrl =
              String.format(
                  JiraRestApi.JIRA_API_PROJECT_PERMISSION_SCHEME_PATTERN,
                  jiraUri,
                  jiraApiPath,
                  openProjectData.getProjectKey());

          verify(jiraRestService, times(1)).httpPut();
          verify(restClientCall, times(1)).url(contains(putPermissionSchemeUrl));
          verify(restClientCall, times(1))
              .body(
                  argThat(
                      obj ->
                          obj instanceof PermissionScheme
                              && ((PermissionScheme) obj).getId().equals(schemeId.toString())));
        };
    return verifyPermissionScheme;
  }

  @NotNull
  private OpenProjectData createOpenProjectData(
      String projectKey, String projectRole, Integer schemeId, String projectTemplateType) {
    OpenProjectData project = new OpenProjectData();
    project.setProjectKey(projectKey);
    project.setProjectType(projectTemplateType);
    project.setProjectAdminGroup("adminGroup");
    project.setProjectUserGroup("userGroup");
    project.setProjectReadonlyGroup("readonlyGroup");
    if (null != schemeId && null != projectRole) {
      project.setProjectRoleForAdminGroup(projectRole);
      project.setProjectRoleForUserGroup(projectRole);
      project.setProjectRoleForReadonlyGroup(projectRole);
      project.setSpecialPermissionSchemeId(schemeId);
    }
    return project;
  }

  @Test
  public void createNewSpecialPermissionsAndUpdateProject() throws IOException {

    OpenProjectData project =
        createOpenProjectData("TEST2", null, null, "unexistant-template-type");

    setupMocks();

    String schemeId = "11111";
    PermissionScheme permissionScheme = mock(PermissionScheme.class);
    when(jiraClient.execute(restClientCall)).thenReturn(permissionScheme);
    when(permissionScheme.getId()).thenReturn(schemeId);
    when(restClientCall.returnTypeReference(any())).thenReturn(restClientCall);

    jiraSpecialPermissioner.setupProjectSpecialPermissions(jiraRestService, project);

    String postNewPermissionSchemeUrl =
        String.format(JiraRestApi.JIRA_API_PERMISSION_SCHEME_PATTERN, jiraUri, jiraApiPath);
    verify(jiraRestService, times(1)).httpPost();
    verify(restClientCall, times(1)).url(contains(postNewPermissionSchemeUrl));

    String putPermissionSchemeUrl =
        String.format(
            JiraRestApi.JIRA_API_PROJECT_PERMISSION_SCHEME_PATTERN,
            jiraUri,
            jiraApiPath,
            project.getProjectKey());

    verify(jiraRestService, times(1)).httpPut();
    verify(restClientCall, times(1)).url(contains(putPermissionSchemeUrl));

    resetMocks();
  }
}
