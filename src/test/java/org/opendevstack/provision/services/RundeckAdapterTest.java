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

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RundeckJobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class RundeckAdapterTest {

  private static final String COMPONENT_ID_KEY = "component_id";
  private static final String COMPONENT_ID = "2";
  private static final String PROJECT_ID = "1";
  private static final String PROJECT_KEY = "123key";

  @Mock
  RundeckJobStore jobStore;

  @Mock
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  @InjectMocks
  RundeckAdapter rundeckAdapter;

  @Mock
  RestClient client;
  
  @Captor
  private ArgumentCaptor<Object> captor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void getQuickstarter() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);
    Job a = Mockito.mock(Job.class);
    List<Job> jobList = new ArrayList<>();
    jobList.add(a);
    doReturn(jobList).when(spyAdapter).getJobs(any());
    int expectedQuickstarterSize = 1;
    
    int actualQuickstarterSize = spyAdapter.getQuickstarter().size();
    
    assertEquals(expectedQuickstarterSize, actualQuickstarterSize);
  }

  @Test
  public void executeJobsWhenQuickstartIsNull() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);
    ProjectData project = new ProjectData();
    List<ExecutionsData> expectedExecutions = new ArrayList<>();
    Mockito.doNothing().when(spyAdapter).authenticate();

    List<ExecutionsData> actualExecutions = spyAdapter.executeJobs(project);

    assertEquals(expectedExecutions, actualExecutions);
  }


  @Test
  public void executeJobs() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);

    ProjectData project = new ProjectData();
    project.key = PROJECT_KEY;
    
    Job job = new Job();
    job.setId(PROJECT_ID);

    Map<String, String> testjob = new HashMap<>();
    testjob.put(RundeckAdapter.COMPONENT_ID_KEY, COMPONENT_ID);
    testjob.put(RundeckAdapter.COMPONENT_TYPE_KEY, COMPONENT_ID);

    List<Map<String, String>> quickstart = new ArrayList<>();

    quickstart.add(testjob);
    project.quickstart = quickstart;

    Execution exec = generateDefaultExecution();

    Mockito.doNothing().when(spyAdapter).authenticate();

    when(jobStore.getJob(anyString())).thenReturn(job);

    mockRestClientToReturnExecutionData(Execution.class, ExecutionsData.class);

    int expectedExecutionsSize = 1;
    
    int actualExecutionsSize = spyAdapter.executeJobs(project).size();

    assertEquals(expectedExecutionsSize, actualExecutionsSize);
  }

  private void mockRestClientToReturnExecutionData(Class input, Class output) throws java.io.IOException {
    Object data = mock(output);
    when(client.callHttp(anyString(), any(input), eq(null), anyBoolean(), eq(RestClient.HTTP_VERB.POST),
            any())).thenReturn(data);
  }

  @Test
  public void createOpenshiftProjects() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);

    ProjectData projectData = new ProjectData();
    projectData.key = "key";
    String crowdCookie = "cookie2";

    ExecutionsData execData = Mockito.mock(ExecutionsData.class);

    Job job1 = new Job();
    job1.setName("create-projects");
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);

    String userNameFromCrowd = "crowdUsername";
    
    CrowdUserDetails details = Mockito.mock(CrowdUserDetails.class);
    when(crowdUserDetailsService.loadUserByToken(crowdCookie)).thenReturn(details);
    when(details.getUsername()).thenReturn(userNameFromCrowd);

    doReturn(jobs).when(spyAdapter).getJobs(any());

    mockRestClientToReturnExecutionData(Execution.class, ExecutionsData.class);

    ProjectData expectedProjectData = generateDefaultProjectData();

    ProjectData createdProjectData = spyAdapter.createOpenshiftProjects(projectData, crowdCookie);

    Execution execution = new Execution();
    Map<String, String> options = new HashMap<>();
    	options.put("project_id", projectData.key);
    	options.put("project_admin", userNameFromCrowd);
    execution.setOptions(options);

    // called once -positive
    Mockito.verify(client).callHttp
    	(any(), refEq( execution), any(), anyBoolean(),
			eq(RestClient.HTTP_VERB.POST),
			any());

	options.put("project_admin", "crowdUsername-WRONG");
    Mockito.verify(client, Mockito.never()).callHttp
		(any(), refEq( execution), any(), anyBoolean(),
			eq(RestClient.HTTP_VERB.POST),
			any());
    
    assertEquals(expectedProjectData, createdProjectData);
    assertTrue(expectedProjectData.openshiftproject);
    assertEquals(expectedProjectData.openshiftConsoleDevEnvUrl, 
    		createdProjectData.openshiftConsoleDevEnvUrl);
    assertEquals(expectedProjectData.openshiftConsoleTestEnvUrl, 
    		createdProjectData.openshiftConsoleTestEnvUrl);
    assertEquals(expectedProjectData.openshiftJenkinsUrl, 
    		createdProjectData.openshiftJenkinsUrl);
  }

  @Test (expected = IOException.class)
  public void createNullOCProject() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);
    spyAdapter.createOpenshiftProjects(null, null);
  }

  @Test
  public void createOpenshiftProjectsWithPassedAdminAndRoles() throws Exception {
    RundeckAdapter spyAdapter = Mockito.spy(rundeckAdapter);

    ProjectData projectData = new ProjectData();
    projectData.key = "key";
    String crowdCookie = "cookie2";

    ExecutionsData execData = Mockito.mock(ExecutionsData.class);

    Job job1 = new Job();
    job1.setName("create-projects");
    Job job2 = new Job();
    job2.setName("name2");

    List<Job> jobs = new ArrayList<>();
    jobs.add(job1);
    jobs.add(job2);
  
    // create special permissionset - here crowd userdetails should never be called
    projectData.createpermissionset = true;
    projectData.admin = "clemens";
    projectData.adminGroup = "agroup";
    projectData.userGroup = "ugroup";
    projectData.readonlyGroup = "rgroup";

//    projectData.adminGroup = "agroup";
//    projectData.userGroup = "ugroup";
//    projectData.readonlyGroup = "rgroup";

    spyAdapter = Mockito.spy(rundeckAdapter);
        
    Mockito.doNothing().when(spyAdapter).authenticate();
    doReturn(jobs).when(spyAdapter).getJobs(any());
    doReturn(execData).when(client).callHttp(anyString(), any(),
    	any(), anyBoolean(),
    	eq(RestClient.HTTP_VERB.POST), any());
    
    spyAdapter.createOpenshiftProjects(projectData, crowdCookie);
    Mockito.verify(crowdUserDetailsService, Mockito.never()).loadUserByToken(crowdCookie); 

    Mockito.verify(client).callHttp
            (any(), captor.capture(), any(), anyBoolean(),
                    eq(RestClient.HTTP_VERB.POST),
                    any());

    Execution execVerify = (Execution)captor.getValue();
    assertNotNull(execVerify);
    assertEquals(execVerify.getOptions().get("project_id"), projectData.key);
    assertEquals(execVerify.getOptions().get("project_admin"), projectData.admin);
    String groups = execVerify.getOptions().get("project_groups");
    assertNotNull(groups);
    assertTrue(groups.contains("ADMINGROUP=" + projectData.adminGroup) &&
            groups.contains("USERGROUP=" + projectData.userGroup) &&
            groups.contains("READONLYGROUP=" + projectData.readonlyGroup));
  }
  
  @Test
  public void getEndpointAPIPath () throws Exception 
  {
	  assertEquals("http://192.168.56.31:4440/rundeck/api/19", 
		rundeckAdapter.getRundeckAPIPath());
  }
  
  private ProjectData generateDefaultProjectData() {
    ProjectData expected = new ProjectData();
    expected.openshiftConsoleDevEnvUrl = "https://192.168.99.100:8443/console/project/key-dev";
    expected.openshiftConsoleTestEnvUrl = "https://192.168.99.100:8443/console/project/key-test";
    expected.openshiftJenkinsUrl = "https://jenkins-key-cd.192.168.99.100.nip.io";
    expected.jiraconfluencespace = true;
    expected.openshiftproject = true;
    expected.key = "key";
    return expected;
  }
  
  private String objectToJson(Object obj) throws JsonProcessingException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(obj);
    return json;
  }

  private Execution generateDefaultExecution() {
    Execution exec = new Execution();
    Map<String, String> options = new HashMap<>();
    options.put(RundeckAdapter.COMPONENT_ID_KEY, COMPONENT_ID);
    options.put("group_id", String.format("org.opendevstack.%s", PROJECT_KEY));
    options.put("project_id", PROJECT_KEY);
    options.put("package_name", String.format("org.opendevstack.%s.%s", PROJECT_KEY, COMPONENT_ID));
    exec.setOptions(options);
    return exec;
  }

  
  
}
