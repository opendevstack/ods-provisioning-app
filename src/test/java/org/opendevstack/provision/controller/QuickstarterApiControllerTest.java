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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.RundeckAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** @author Torsten Jaeschke */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class QuickstarterApiControllerTest {
  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;

  @MockBean RundeckAdapter rundeckAdapter;

  private List<Job> jobs;
  private OpenProjectData project;
  private List<ExecutionsData> executions = new ArrayList<>();

  @Before
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
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
    Mockito.when(rundeckAdapter.getQuickstarters()).thenReturn(jobs);

    mockMvc
        .perform(get("/api/v1/quickstarter"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
  }

  @Test
  public void executeJobs() throws Exception {
    Mockito.when(rundeckAdapter.provisionComponentsBasedOnQuickstarters(project))
        .thenReturn(executions);

    mockMvc
        .perform(
            post("/api/v1/quickstarter/provision")
                .content(getBody())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
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
