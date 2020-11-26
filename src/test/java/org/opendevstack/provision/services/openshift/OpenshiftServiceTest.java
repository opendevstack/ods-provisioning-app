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
package org.opendevstack.provision.services.openshift;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
@ActiveProfiles("utest")
public class OpenshiftServiceTest {

  @MockBean private OpenshiftClient openshiftClient;

  @Autowired private OpenshiftService openshiftService;

  @Before
  public void setup() {
    openshiftService = new OpenshiftService(openshiftClient);
  }

  @Test
  public void givenCheckProjectKey_whenProjectKeyPrefixExists_thenFailure()
      throws CreateProjectPreconditionException {

    Set<String> openshiftProjects = Set.of("testp", "TESTP-cd", "TEStp-dev", "testp-test");
    when(openshiftClient.projects()).thenReturn(openshiftProjects);
    String project = "TESTP";

    // case project key with pattern "<PROJECT_NAME>-*" does exist
    List<CheckPreconditionFailure> failures = openshiftService.createProjectKeyExistsCheck(project);
    assertNotNull(CheckPreconditionFailure.ExceptionCodes.valueOf(failures.get(0).getCode()));
    assertTrue(failures.get(0).getDetail().contains(project));

    // case project key with pattern "<PROJECT_NAME>-*" does not exist
    failures = openshiftService.createProjectKeyExistsCheck("UNEXISTANT_PROJECT");
    assertTrue(failures.size() == 0);
  }

  @Test
  public void givenCheckProjectKey_whenProjectKeyPrefixDoesNotExists_thenNoFailure()
      throws CreateProjectPreconditionException {

    when(openshiftClient.projects())
        .thenReturn(Set.of("project-cd", "project-dev", "project-test", "ods", "default"));

    // case project key with pattern "<PROJECT_NAME>-*" does not exist
    List<CheckPreconditionFailure> failures =
        openshiftService.createProjectKeyExistsCheck("UNEXISTANT_PROJECT");
    assertTrue(failures.size() == 0);
  }

  @Test
  public void givenCheckProjectKey_whenProjectKeyExistsButNotAsPrefix_thenNoFailure()
      throws CreateProjectPreconditionException {

    String notOdsProject = "NOT_ODS_PROJECT";

    // ods project are composed of 3 namespaces/project in openshift -> they start with a prefix in
    // common with this pattern "<PROJECT_NAME>-*"
    // special case: project key exists but not as prefix
    when(openshiftClient.projects())
        .thenReturn(
            Set.of(notOdsProject + "odsproj-cd", "odsproj-dev", "odsproj-test", "ods", "default"));

    // case project key with pattern "<PROJECT_NAME>-*" does not exist
    List<CheckPreconditionFailure> failures =
        openshiftService.createProjectKeyExistsCheck("NOT_ODS_PROJECT");
    assertTrue(failures.size() == 0);
  }

  @Test
  public void givenCheckProjectKey_whenException_thenAdapterExceptionIsRaised()
      throws CreateProjectPreconditionException {

    String notOdsProject = "NOT_ODS_PROJECT";

    // ods project are composed of 3 namespaces/project in openshift -> they start with a prefix in
    // common with this pattern "<PROJECT_NAME>-*"
    // special case: project key exists but not as prefix
    when(openshiftClient.projects()).thenThrow(new RuntimeException("exception raised in test"));

    // case project key with pattern "<PROJECT_NAME>-*" does not exist
    try {
      List<CheckPreconditionFailure> failures =
          openshiftService.createProjectKeyExistsCheck("NOT_ODS_PROJECT");
      assertTrue(failures.size() == 0);
      fail();
    } catch (AdapterException ex) {
      // expected exception
    }
  }

  @Test
  public void givenCheckCreateProjectPreconditions_whenUnexceptedError_thenExceptionIsRaised()
      throws CreateProjectPreconditionException {

    OpenProjectData projectData = new OpenProjectData();

    try {
      // case unexpected exception
      openshiftService.checkCreateProjectPreconditions(projectData);
    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getMessage().contains("Unexpected error"));
    }

    try {
      // case adapter exception
      projectData.setProjectKey("TESTP");
      when(openshiftClient.projects()).thenThrow(new RuntimeException("exception raised in test"));
      openshiftService.checkCreateProjectPreconditions(projectData);
    } catch (CreateProjectPreconditionException e) {
      assertTrue(e.getCause() instanceof AdapterException);
      assertTrue(e.getMessage().contains(projectData.getProjectKey()));
    }
  }

  @Test
  public void givenCheckCreateProjectPreconditions_whenOpenshiftClientReturnsListOfProjects_thenOK()
      throws CreateProjectPreconditionException {

    OpenProjectData projectData = new OpenProjectData();
    projectData.setProjectKey("TESTP");

    // case no exception
    when(openshiftClient.projects()).thenReturn(Set.of("default", "ods"));
    List<CheckPreconditionFailure> checkPreconditionFailures =
        openshiftService.checkCreateProjectPreconditions(projectData);
    assertEquals(0, checkPreconditionFailures.size());
  }
}
