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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.authentication.UserRolesHolder;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("utest")
@DirtiesContext
public class ApplicationInfoAPIControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StorageAdapter storageAdapter;

  @MockBean private UserRolesHolder userRolesHolder;

  @MockBean private IODSAuthnzAdapter mockAuthnzAdapter;

  @MockBean private IBugtrackerAdapter jiraAdapter;

  @MockBean private ISCMAdapter bitbucketAdapter;

  @MockBean private ICollaborationAdapter confluenceAdapter;

  @Value("${idmanager.group.opendevstack-users}")
  private String idmanagerUserGroup;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String idmanagerAdminGroup;

  @BeforeEach
  public void setup() {
    when(mockAuthnzAdapter.getUserName()).thenReturn(TEST_ADMIN_USERNAME);
    when(mockAuthnzAdapter.getUserEmail()).thenReturn(TEST_ADMIN_EMAIL);
    when(mockAuthnzAdapter.getUserPassword()).thenReturn(TEST_VALID_CREDENTIAL);
  }

  @Test
  public void givenAboutRoute_whenGetRequest_thenReturnResponse() throws Exception {

    when(confluenceAdapter.getAdapterApiUri()).thenReturn("uri");
    when(jiraAdapter.getAdapterApiUri()).thenReturn("uri");
    when(bitbucketAdapter.getAdapterApiUri()).thenReturn("uri");
    when(userRolesHolder.getUserRoles()).thenReturn(Set.of("role"));

    ResultMatcher username =
        MockMvcResultMatchers.jsonPath("username", CoreMatchers.is(TEST_ADMIN_USERNAME));
    ResultMatcher email =
        MockMvcResultMatchers.jsonPath("email", CoreMatchers.is(TEST_ADMIN_EMAIL));
    ResultMatcher userRoles =
        MockMvcResultMatchers.jsonPath("userRoles", CoreMatchers.hasItem("role"));
    ResultMatcher endpoints =
        MockMvcResultMatchers.jsonPath("endpoints", CoreMatchers.notNullValue());
    ResultMatcher userGroup =
        MockMvcResultMatchers.jsonPath(
            "idmanagerUserGroup", CoreMatchers.is(idmanagerUserGroup.toLowerCase()));
    ResultMatcher adminGroup =
        MockMvcResultMatchers.jsonPath(
            "idmanagerAdminGroup", CoreMatchers.is(idmanagerAdminGroup.toLowerCase()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/" + ApplicationInfoAPI.ABOUT_APP_INFO_API_V2)
                .with(httpBasic(TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(username)
        .andExpect(email)
        .andExpect(userRoles)
        .andExpect(endpoints)
        .andExpect(userGroup)
        .andExpect(adminGroup)
        .andReturn();
  }

  @Test
  public void givenHistoryRoute_whenGetRequest_thenReturnResponse() throws Exception {

    // setup test
    OpenProjectData data = new OpenProjectData();
    data.setProjectKey("KEY");

    Map<String, OpenProjectData> historyMap = new HashMap<>();
    historyMap.put(data.getProjectKey(), data);

    when(storageAdapter.listProjectHistory()).thenReturn(historyMap);

    // create matchers
    ResultMatcher projectHistory =
        MockMvcResultMatchers.jsonPath("projectHistory", CoreMatchers.notNullValue());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/" + ApplicationInfoAPI.HISTORY_APP_INFO_API_V2)
                .with(httpBasic(TEST_ADMIN_USERNAME, TEST_VALID_CREDENTIAL))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(projectHistory)
        .andReturn();
  }
}
