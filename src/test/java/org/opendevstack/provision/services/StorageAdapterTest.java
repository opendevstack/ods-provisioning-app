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

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.LocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

/** @author Torsten Jaeschke */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class StorageAdapterTest {

  @Mock LocalStorage storage;

  @Autowired private WebApplicationContext context;

  @Autowired StorageAdapter adapter;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void listProjectHistoryNoAuth() throws Exception {
    Mockito.when(storage.listProjectHistory()).thenReturn(new HashMap<>());
    adapter.setStorage(storage);

    assertTrue(adapter.listProjectHistory().isEmpty());
  }

  @Test
  public void listProjectHistoryWithAuth() throws Exception {
    try {
      // open project
      OpenProjectData data = new OpenProjectData();
      data.projectName = "testproject";
      data.projectKey = "Z_KEY";
      data.projectAdminGroup = "testgroup";

      // case sensitive right group
      OpenProjectData dataProtected = new OpenProjectData();
      dataProtected.projectName = "testprojectProtected";
      dataProtected.projectKey = "A_KEY";
      dataProtected.projectAdminGroup = "testgroup";
      dataProtected.specialPermissionSet = true;

      // wrong group
      OpenProjectData dataProtectedWrong = new OpenProjectData();
      dataProtectedWrong.projectName = "testprojectProtectedW";
      dataProtectedWrong.projectKey = "testprojectProtectedW";
      dataProtectedWrong.projectAdminGroup = "testgroupW";
      dataProtectedWrong.specialPermissionSet = true;

      // group upper lower case
      OpenProjectData dataProtectedCase = new OpenProjectData();
      dataProtectedCase.projectName = "testprojectProtectedC";
      dataProtectedCase.projectKey = "F_KEY";
      dataProtectedCase.projectAdminGroup = "testGroup";
      dataProtectedCase.specialPermissionSet = true;

      Map<String, OpenProjectData> projects = new HashMap<>();
      projects.put(data.projectKey, data);
      projects.put(dataProtected.projectKey, dataProtected);
      projects.put(dataProtectedWrong.projectKey, dataProtectedWrong);
      projects.put(dataProtectedCase.projectKey, dataProtectedCase);

      Mockito.when(storage.listProjectHistory()).thenReturn(projects);
      adapter.setStorage(storage);

      SecurityContextHolder.getContext().setAuthentication(new TestAuthentication());

      Map<String, OpenProjectData> testresult = adapter.listProjectHistory();
      assertEquals(3, testresult.size());
      assertEquals(
          Lists.newArrayList("A_KEY", "F_KEY", "Z_KEY"), new ArrayList<>(testresult.keySet()));
      assertFalse(testresult.containsKey(dataProtectedWrong.projectKey));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
