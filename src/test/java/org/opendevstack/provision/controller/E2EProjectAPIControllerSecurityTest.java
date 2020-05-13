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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.basic.BasicAuthSecurityTestConfig;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This E2E test focus in testing 2 things: - basic auth authentication - authorization negative
 * cases.
 *
 * <p>NOTES: - positive cases are tested in E2EProjectAPIControllerTest - see
 * BasicAuthSecurityConfig.class, it configures a TestingAuthenticationProvider with test users
 *
 * @author Sebastian Titakis
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SpringBoot.class)
@ActiveProfiles("utest")
public class E2EProjectAPIControllerSecurityTest {

  public static final String TEST_VALID_CREDENTIAL =
      BasicAuthSecurityTestConfig.TEST_VALID_CREDENTIAL;

  @MockBean private StorageAdapter storageAdapter;

  @Autowired private TestRestTemplate template;

  private final String admin = BasicAuthSecurityTestConfig.TEST_ADMIN_USERNAME;
  private final String user = BasicAuthSecurityTestConfig.TEST_USER_USERNAME;
  private final String unknownUser =
      BasicAuthSecurityTestConfig
          .TEST_NOT_PERMISSIONED_USER_USERNAME; // User that does not have any ProvApp role
  private final String validCredential = BasicAuthSecurityTestConfig.TEST_VALID_CREDENTIAL;
  private final String invalidCredential = "invalidSecret";
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

  private HttpHeaders headers = new HttpHeaders();

  private OpenProjectData projectData;

  @Before
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

    assertRequest(getProjectRequest, user, invalidCredential, HttpStatus.UNAUTHORIZED);
    assertRequest(getProjectRequest, admin, invalidCredential, HttpStatus.UNAUTHORIZED);
    assertRequest(getProjectRequest, unknownUser, invalidCredential, HttpStatus.UNAUTHORIZED);
    assertRequest(getProjectRequest, user, validCredential, HttpStatus.OK);
    assertRequest(getProjectRequest, admin, validCredential, HttpStatus.OK);
    assertRequest(getProjectRequest, unknownUser, validCredential, HttpStatus.FORBIDDEN);
  }

  public BiFunction<String, String, HttpStatus> createGetProjectRequest() {

    BiFunction<String, String, HttpStatus> request =
        (username, credential) -> {
          OpenProjectData project = new OpenProjectData();
          project.projectKey = "testproject";
          Map<String, OpenProjectData> projects = new HashMap<>();
          projects.put(project.projectKey, project);
          when(storageAdapter.getProjects()).thenReturn(projects);

          ResponseEntity<Map> entity =
              template.withBasicAuth(username, credential).getForEntity(projectAPI, Map.class);

          return entity.getStatusCode();
        };

    return request;
  }

  public BiFunction<String, String, HttpStatus> postNewProject() throws Exception {

    BiFunction<String, String, HttpStatus> request =
        (username, credential) -> {
          String json = ProjectApiControllerTest.asJsonString(projectData);

          HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);

          ResponseEntity<Object> entity =
              template
                  .withBasicAuth(username, credential)
                  .postForEntity(projectAPI, httpRequest, Object.class);

          return entity.getStatusCode();
        };
    return request;
  }

  @Test
  public void authorizationForbiddenTest() throws Exception {

    // not permissioned user = user has not ProvApp roles
    assertRequest(postNewProject(), unknownUser, validCredential, HttpStatus.FORBIDDEN);
    assertRequest(updateProject(), unknownUser, validCredential, HttpStatus.FORBIDDEN);
    assertRequest(deleteProject(), unknownUser, validCredential, HttpStatus.FORBIDDEN);
    assertRequest(deleteComponent(), unknownUser, validCredential, HttpStatus.FORBIDDEN);
    assertRequests(
        allGetAPIs, getRequestFactory(), unknownUser, validCredential, HttpStatus.FORBIDDEN);

    // ProvApp user
    assertRequest(postNewProject(), user, validCredential, HttpStatus.FORBIDDEN);
    assertRequest(deleteProject(), user, validCredential, HttpStatus.FORBIDDEN);
    assertRequest(deleteComponent(), user, validCredential, HttpStatus.FORBIDDEN);
  }

  public BiFunction<String, String, HttpStatus> updateProject() throws Exception {

    BiFunction<String, String, HttpStatus> request =
        (username, credential) -> {
          String json = ProjectApiControllerTest.asJsonString(projectData);

          HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);

          ResponseEntity exchange =
              template
                  .withBasicAuth(username, credential)
                  .getRestTemplate()
                  .exchange(projectAPI, HttpMethod.PUT, httpRequest, Object.class, new HashMap());

          return exchange.getStatusCode();
        };
    return request;
  }

  public BiFunction<String, String, HttpStatus> deleteProject() throws Exception {

    BiFunction<String, String, HttpStatus> request =
        (username, credential) -> {
          String json = ProjectApiControllerTest.asJsonString(projectData);

          HttpEntity<String> httpRequest = new HttpEntity<>(json, headers);

          ResponseEntity exchange =
              template
                  .withBasicAuth(username, credential)
                  .getRestTemplate()
                  .exchange(
                      projectAPI, HttpMethod.DELETE, httpRequest, Object.class, new HashMap());
          return exchange.getStatusCode();
        };
    return request;
  }

  public BiFunction<String, String, HttpStatus> deleteComponent() throws Exception {

    BiFunction<String, String, HttpStatus> request =
        (username, credential) -> {
          String api = projectAPI + "/PROJECTKEY";
          ResponseEntity exchange =
              template
                  .withBasicAuth(username, credential)
                  .getRestTemplate()
                  .exchange(api, HttpMethod.DELETE, null, Object.class, new HashMap());
          return exchange.getStatusCode();
        };
    return request;
  }

  public Function<String, BiFunction<String, String, HttpStatus>> getRequestFactory() {
    return api -> {
      return (username, credential) -> {
        ResponseEntity<Map> entity =
            template
                .withBasicAuth(
                    BasicAuthSecurityTestConfig.TEST_NOT_PERMISSIONED_USER_USERNAME,
                    TEST_VALID_CREDENTIAL)
                .getForEntity(api, Map.class);
        return entity.getStatusCode();
      };
    };
  }

  public void assertRequest(
      BiFunction<String, String, HttpStatus> request,
      String username,
      String credential,
      HttpStatus expected) {

    HttpStatus responseStatus = request.apply(username, credential);
    assertEquals(
        String.format(
            "Authentication failed with unexpected return code for user '%s:%s' ",
            username, credential),
        expected,
        responseStatus);
  }

  public void assertRequests(
      List<String> apis,
      Function<String, BiFunction<String, String, HttpStatus>> requestFactory,
      String username,
      String credential,
      HttpStatus expected) {

    apis.forEach(
        endpoint -> {
          assertRequest(requestFactory.apply(endpoint), username, credential, expected);
        });
  }
}
