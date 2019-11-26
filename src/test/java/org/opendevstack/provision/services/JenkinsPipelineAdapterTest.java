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

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jenkins.Execution;
import org.opendevstack.provision.model.jenkins.Job;

import org.opendevstack.provision.util.ValueCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Torsten Jaeschke */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class JenkinsPipelineAdapterTest extends AbstractBaseServiceAdapterTest {

  private static final String COMPONENT_ID = "2";
  private static final String PROJECT_ID = "1";
  private static final String PROJECT_KEY = "123key";

  @Autowired CrowdAuthenticationManager manager;

  @Autowired @InjectMocks JenkinsPipelineAdapter jenkinsPipelineAdapter;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    super.beforeTest();
  }

  @Test
  public void getQuickstarter() throws Exception {
    JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);

    String group = "testgroup";


    List<Job> jobList = asList(Mockito.mock(Job.class));

    int expectedQuickstarterSize = jenkinsPipelineAdapter.getJenkinsPipelineQuickstarter().size();

    int actualQuickstarterSize = spyAdapter.getQuickstarters().size();

    assertEquals(expectedQuickstarterSize, actualQuickstarterSize);
  }

  @Test
  public void executeJobsWhenQuickstartIsNull() throws Exception {
    JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);
    OpenProjectData project = new OpenProjectData();
    List<ExecutionsData> expectedExecutions = new ArrayList<>();

    ArrayList<Job> jobs = new ArrayList<>();
    mockJobsInServer(jobs);

    List<ExecutionsData> actualExecutions =
        spyAdapter.provisionComponentsBasedOnQuickstarters(project);

    assertEquals(expectedExecutions, actualExecutions);
  }

  @Test
  public void executeJobs() throws Exception {

    OpenProjectData project = new OpenProjectData();
    project.projectKey = PROJECT_KEY;

    Job job = new Job();
    job.setId(PROJECT_ID);

    Map<String, String> testjob = new HashMap<>();
    testjob.put(OpenProjectData.COMPONENT_ID_KEY, COMPONENT_ID);
    testjob.put(OpenProjectData.COMPONENT_TYPE_KEY, COMPONENT_ID);

    List<Map<String, String>> quickstart = new ArrayList<>();

    quickstart.add(testjob);
    project.quickstarters = quickstart;

    Execution exec = generateDefaultExecution();

    mockJobsInServer(asList(job));

    mockRestClientToReturnExecutionData(Execution.class, ExecutionsData.class);

    int expectedExecutionsSize = 1;

    int actualExecutionsSize =
        jenkinsPipelineAdapter.provisionComponentsBasedOnQuickstarters(project).size();

    assertEquals(expectedExecutionsSize, actualExecutionsSize);
  }

  private void mockRestClientToReturnExecutionData(Class input, Class output)
      throws java.io.IOException {
    Object data = mock(output);

    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(data);
  }

  @Test
  public void createOpenshiftProjects() throws Exception {

    OpenProjectData projectData = new OpenProjectData();
    projectData.projectKey = "key";

    Job job1 = new Job();
    job1.setName("create-projects");
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);

    String userNameFromCrowd = "crowdUsername";

    mockJobsInServer(jobs);

    mockRestClientToReturnExecutionData(Execution.class, ExecutionsData.class);

    OpenProjectData expectedOpenProjectData = generateDefaultOpenProjectData();
    manager.setUserName(userNameFromCrowd);

    OpenProjectData createdOpenProjectData = jenkinsPipelineAdapter.createPlatformProjects(projectData);

    Execution execution = new Execution();
    Map<String, String> options = new HashMap<>();
    options.put("project_id", projectData.projectKey);
    options.put("project_admin", userNameFromCrowd);
    execution.setOptions(options);

    verifyExecute(
        matchesClientCall().method(HttpMethod.POST).bodyMatches(samePropertyValuesAs(execution)));

    options.put("project_admin", "crowdUsername-WRONG");
    verifyExecute(
        matchesClientCall().method(HttpMethod.POST).bodyMatches(samePropertyValuesAs(execution)),
        never());
    assertEquals(expectedOpenProjectData, createdOpenProjectData);
    assertTrue(expectedOpenProjectData.platformRuntime);
    assertEquals(
        expectedOpenProjectData.platformDevEnvironmentUrl,
        createdOpenProjectData.platformDevEnvironmentUrl);
    assertEquals(
        expectedOpenProjectData.platformTestEnvironmentUrl,
        createdOpenProjectData.platformTestEnvironmentUrl);
    assertEquals(
        expectedOpenProjectData.platformBuildEngineUrl,
        createdOpenProjectData.platformBuildEngineUrl);
  }

  public void mockJobsInServer(List<Job> jobs) throws IOException {
    mockExecute(matchesClientCall().method(HttpMethod.GET)).thenReturn(jobs);
  }

  @Test(expected = NullPointerException.class)
  public void createNullOCProject() throws Exception {
    JenkinsPipelineAdapter spyAdapter = Mockito.spy(jenkinsPipelineAdapter);
    spyAdapter.createPlatformProjects(null);
  }

  @Test
  public void createOpenshiftProjectsWithPassedAdminAndRoles() throws Exception {

    OpenProjectData projectData = new OpenProjectData();
    projectData.projectKey = "key";

    ExecutionsData execData = new ExecutionsData();

    Job job1 = new Job();
    job1.setName("create-projects");
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);

    // create special permissionset - here crowd userdetails should never be called
    projectData.specialPermissionSet = true;
    projectData.projectAdminUser = "clemens";
    projectData.projectAdminGroup = "agroup";
    projectData.projectUserGroup = "ugroup";
    projectData.projectReadonlyGroup = "rgroup";

    mockJobsInServer(jobs);


    mockExecute(matchesClientCall().method(HttpMethod.POST)).thenReturn(execData);

    jenkinsPipelineAdapter.createPlatformProjects(projectData);

    ValueCaptor<Object> valueHolder = new ValueCaptor<>();
    verifyExecute(matchesClientCall().method(HttpMethod.POST).bodyCaptor(valueHolder));

    Execution execVerify = (Execution) valueHolder.getValues().get(0);
    assertNotNull(execVerify);
    assertEquals(execVerify.getOptions().get("project_id"), projectData.projectKey);
    assertEquals(execVerify.getOptions().get("project_admin"), projectData.projectAdminUser);
    String groups = execVerify.getOptions().get("project_groups");
    assertNotNull(groups);
    assertTrue(
        groups.contains("ADMINGROUP=" + projectData.projectAdminGroup)
            && groups.contains("USERGROUP=" + projectData.projectUserGroup)
            && groups.contains("READONLYGROUP=" + projectData.projectReadonlyGroup));
  }

  @Test
  public void getEndpointAPIPath() throws Exception {
    assertEquals("http://192.168.56.31:4440/api/19", jenkinsPipelineAdapter.getAdapterApiUri());
  }

  private OpenProjectData generateDefaultOpenProjectData() {
    OpenProjectData expected = new OpenProjectData();
    expected.platformDevEnvironmentUrl = "https://192.168.56.101:8443/console/project/key-dev";
    expected.platformTestEnvironmentUrl = "https://192.168.56.101:8443/console/project/key-test";
    expected.platformBuildEngineUrl = "https://jenkins-key-cd.192.168.56.101.nip.io";
    expected.bugtrackerSpace = true;
    expected.platformRuntime = true;
    expected.projectKey = "key";
    return expected;
  }

  private Execution generateDefaultExecution() {
    Execution exec = new Execution();
    Map<String, String> options = new HashMap<>();
    options.put(OpenProjectData.COMPONENT_ID_KEY, COMPONENT_ID);
    options.put("group_id", String.format("org.opendevstack.%s", PROJECT_KEY));
    options.put("project_id", PROJECT_KEY);
    options.put("package_name", String.format("org.opendevstack.%s.%s", PROJECT_KEY, COMPONENT_ID));
    exec.setOptions(options);
    return exec;
  }
}
