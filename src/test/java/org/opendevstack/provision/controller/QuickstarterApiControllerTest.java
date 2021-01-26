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
import static org.opendevstack.provision.config.AuthSecurityTestConfig.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jenkins.Job;
import org.opendevstack.provision.services.JenkinsPipelineAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@DirtiesContext
@AutoConfigureMockMvc
@ActiveProfiles("utest")
public class QuickstarterApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JenkinsPipelineAdapter jenkinsPipelineAdapter;

  @MockBean private IODSAuthnzAdapter mockAuthnzAdapter;

  private List<Job> jobs;
  private OpenProjectData project;

  @BeforeEach
  public void init() {
    when(mockAuthnzAdapter.getUserName()).thenReturn(TEST_ADMIN_USERNAME);
    when(mockAuthnzAdapter.getUserPassword()).thenReturn(TEST_VALID_CREDENTIAL);

    initJobs();
  }

  private void initJobs() {
    project = new OpenProjectData();
    project.setProjectKey("TST");
    project.setProjectName("name");

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
        .thenReturn(List.of());

    // authorized
    mockMvc
        .perform(
            post("/api/v1/quickstarter/provision")
                .with(httpBasic(TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL))
                .content(getBody())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andReturn()
        .getResponse();

    // not authorized
    mockMvc
        .perform(
            post("/api/v1/quickstarter/provision")
                .with(httpBasic(TEST_NOT_PERMISSIONED_USER_USERNAME, TEST_VALID_CREDENTIAL))
                .content(getBody())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden())
        .andReturn()
        .getResponse();
  }

  private String getBody() throws Exception {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    return ow.writeValueAsString(project);
  }
}
