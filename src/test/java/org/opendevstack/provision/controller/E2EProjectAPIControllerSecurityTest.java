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
package org.opendevstack.provision.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.opendevstack.provision.config.AuthSecurityTestConfig.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * This E2E test focus in testing 2 things: - basic auth authentication - authorization negative
 * cases.
 *
 * <p>NOTES: - positive cases are tested in E2EProjectAPIControllerTest - see
 * BasicAuthSecurityConfig.class, it configures a TestingAuthenticationProvider with test users
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles("utest")
public class E2EProjectAPIControllerSecurityTest {

  private final String projectAPI = "/" + ProjectAPI.API_V2_PROJECT;
  private final List<String> allGetAPIs =
      List.of(
          projectAPI, // get all projects API
          projectAPI + "/PROJECTKEY",
          projectAPI + "/validate?projectName=PN",
          projectAPI + "/templates",
          projectAPI + "/template/KEY",
          projectAPI + "/key/validate?projectKey=KEY",
          projectAPI + "/key/generate?name=NAME");
  @MockBean private StorageAdapter storageAdapter;
  @Autowired private TestRestTemplate template;
  private HttpHeaders headers = new HttpHeaders();

  private OpenProjectData projectData;

  @BeforeEach
  public void setup() throws Exception {
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    projectData =
        E2EProjectAPIControllerTest.readTestData(
            "ods-create-project-request", OpenProjectData.class);
  }

  @Test
  public void authenticationTest() {
    BiFunction<String, String, HttpStatus> getProjectRequest = createGetProjectRequest();
    String invalidCredential = "invalidSecret";
    assertRequest(
        getProjectRequest, TEST_USER_USERNAME, invalidCredential, HttpStatus.UNAUTHORIZED);
    assertRequest(
        getProjectRequest, TEST_ADMIN_USERNAME, invalidCredential, HttpStatus.UNAUTHORIZED);
    assertRequest(
        getProjectRequest,
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        invalidCredential,
        HttpStatus.UNAUTHORIZED);
    assertRequest(getProjectRequest, TEST_USER_USERNAME, TEST_VALID_CREDENTIAL, HttpStatus.OK);
    assertRequest(getProjectRequest, TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL, HttpStatus.OK);
    assertRequest(
        getProjectRequest,
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);
  }

  public BiFunction<String, String, HttpStatus> createGetProjectRequest() {
    return (username, credential) -> {
      OpenProjectData project = new OpenProjectData();
      project.setProjectKey("testproject");
      Map<String, OpenProjectData> projects = new HashMap<>();
      projects.put(project.getProjectKey(), project);
      when(storageAdapter.getProjects()).thenReturn(projects);

      var entity = template.withBasicAuth(username, credential).getForEntity(projectAPI, Map.class);

      return entity.getStatusCode();
    };
  }

  public BiFunction<String, String, HttpStatus> postNewProject() {
    return (username, credential) -> {
      String json = ProjectApiControllerTest.asJsonString(projectData);
      HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);
      var entity =
          template
              .withBasicAuth(username, credential)
              .postForEntity(projectAPI, httpRequest, Object.class);

      return entity.getStatusCode();
    };
  }

  @Test
  public void authorizationForbiddenTest() {
    // not permissioned user = user has not ProvApp roles
    assertRequest(
        postNewProject(),
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);
    assertRequest(
        updateProject(),
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);
    assertRequest(
        deleteProject(),
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);
    assertRequest(
        deleteComponent(),
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);
    assertRequests(
        allGetAPIs,
        getRequestFactory(),
        TEST_NOT_PERMISSIONED_USER_USERNAME,
        TEST_VALID_CREDENTIAL,
        HttpStatus.FORBIDDEN);

    // ProvApp user
    assertRequest(
        postNewProject(), TEST_USER_USERNAME, TEST_VALID_CREDENTIAL, HttpStatus.FORBIDDEN);
    assertRequest(deleteProject(), TEST_USER_USERNAME, TEST_VALID_CREDENTIAL, HttpStatus.FORBIDDEN);
    assertRequest(
        deleteComponent(), TEST_USER_USERNAME, TEST_VALID_CREDENTIAL, HttpStatus.FORBIDDEN);
  }

  public BiFunction<String, String, HttpStatus> updateProject() {
    return (username, credential) -> {
      String json = ProjectApiControllerTest.asJsonString(projectData);
      HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);
      var exchange =
          template
              .withBasicAuth(username, credential)
              .getRestTemplate()
              .exchange(projectAPI, HttpMethod.PUT, httpRequest, Object.class, Map.of());

      return exchange.getStatusCode();
    };
  }

  public BiFunction<String, String, HttpStatus> deleteProject() {
    return (username, credential) -> {
      String json = ProjectApiControllerTest.asJsonString(projectData);
      HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);
      var exchange =
          template
              .withBasicAuth(username, credential)
              .getRestTemplate()
              .exchange(projectAPI, HttpMethod.DELETE, httpRequest, Object.class, Map.of());
      return exchange.getStatusCode();
    };
  }

  public BiFunction<String, String, HttpStatus> deleteComponent() {
    return (username, credential) -> {
      String api = projectAPI + "/PROJECTKEY";
      var exchange =
          template
              .withBasicAuth(username, credential)
              .getRestTemplate()
              .exchange(api, HttpMethod.DELETE, null, Object.class, Map.of());
      return exchange.getStatusCode();
    };
  }

  public Function<String, BiFunction<String, String, HttpStatus>> getRequestFactory() {
    return api ->
        (username, credential) -> {
          var entity =
              template
                  .withBasicAuth(TEST_NOT_PERMISSIONED_USER_USERNAME, TEST_VALID_CREDENTIAL)
                  .getForEntity(api, Map.class);
          return entity.getStatusCode();
        };
  }

  public void assertRequest(
      BiFunction<String, String, HttpStatus> request,
      String username,
      String credential,
      HttpStatus expected) {
    HttpStatus responseStatus = request.apply(username, credential);
    assertEquals(
        expected,
        responseStatus,
        String.format(
            "Authentication failed with unexpected return code for user '%s:%s' ",
            username, credential));
  }

  public void assertRequests(
      List<String> apis,
      Function<String, BiFunction<String, String, HttpStatus>> requestFactory,
      String username,
      String credential,
      HttpStatus expected) {
    apis.forEach(
        endpoint -> assertRequest(requestFactory.apply(endpoint), username, credential, expected));
  }
}
