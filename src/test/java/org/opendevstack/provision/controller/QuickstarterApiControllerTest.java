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

import static org.mockito.Mockito.when;
import static org.opendevstack.provision.authentication.basic.BasicAuthSecurityTestConfig.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jenkins.Job;
import org.opendevstack.provision.services.JenkinsPipelineAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("utest")
public class QuickstarterApiControllerTest {
  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;

  @Qualifier("testUsersAndRoles")
  @Autowired
  private Map<String, String> testUsersAndRoles;

  @MockBean JenkinsPipelineAdapter jenkinsPipelineAdapter;

  @MockBean private IODSAuthnzAdapter mockAuthnzAdapter;

  private List<Job> jobs;
  private OpenProjectData project;
  private List<ExecutionsData> executions = new ArrayList<>();

  private static final String TEST_ADMIN_EMAIL = "testUserName@example.com";

  @BeforeEach
  public void init() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

    when(mockAuthnzAdapter.getUserName()).thenReturn(TEST_ADMIN_USERNAME);
    when(mockAuthnzAdapter.getUserEmail()).thenReturn(TEST_ADMIN_EMAIL);
    when(mockAuthnzAdapter.getUserPassword()).thenReturn(TEST_VALID_CREDENTIAL);

    initJobs();
  }

  private void initJobs() {
    project = new OpenProjectData();
    project.projectKey = "TST";
    project.projectName = "name";

    jobs = new ArrayList<>();
    Job job = new Job();
    job.setName("Job");
    job.setDescription("Description");
    jobs.add(job);
  }

  @Test
  public void getQuickstarters() throws Exception {
    Mockito.when(jenkinsPipelineAdapter.getQuickstarterJobs()).thenReturn(jobs);

    // authorized
    mockMvc
        .perform(
            get("/api/v1/quickstarter").with(httpBasic(TEST_USER_USERNAME, TEST_VALID_CREDENTIAL)))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

    // not authorized
    mockMvc
        .perform(
            get("/api/v1/quickstarter")
                .with(httpBasic(TEST_NOT_PERMISSIONED_USER_USERNAME, TEST_VALID_CREDENTIAL)))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void executeJobs() throws Exception {
    Mockito.when(jenkinsPipelineAdapter.provisionComponentsBasedOnQuickstarters(project))
        .thenReturn(executions);

    // authorized
    mockMvc
        .perform(
            post("/api/v1/quickstarter/provision")
                .with(httpBasic(TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL))
                .content(getBody())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andReturn()
        .getResponse()
        .toString();

    // not authorized
    mockMvc
        .perform(
            post("/api/v1/quickstarter/provision")
                .with(httpBasic(TEST_NOT_PERMISSIONED_USER_USERNAME, TEST_VALID_CREDENTIAL))
                .content(getBody())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden())
        .andReturn()
        .getResponse()
        .toString();
  }

  private String getBody() throws Exception {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(project);
    return json;
  }
}
