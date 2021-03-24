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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.LocalStorage;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class StorageAdapterTest {

  @InjectMocks private StorageAdapter adapter;

  @Mock private LocalStorage storage;

  @Test
  public void listProjectHistoryNoAuth() {
    Mockito.when(storage.listProjectHistory()).thenReturn(new HashMap<>());
    adapter.setStorage(storage);

    assertTrue(adapter.listProjectHistory().isEmpty());
  }

  @Test
  public void listProjectHistoryWithAuth() {
    try {
      // open project
      OpenProjectData data = new OpenProjectData();
      data.setProjectName("testproject");
      data.setProjectKey("Z_KEY");
      data.setProjectAdminGroup("testgroup");

      // case sensitive right group
      OpenProjectData dataProtected = new OpenProjectData();
      dataProtected.setProjectName("testprojectProtected");
      dataProtected.setProjectKey("A_KEY");
      dataProtected.setProjectAdminGroup("testgroup");
      dataProtected.setSpecialPermissionSet(true);

      // wrong group
      OpenProjectData dataProtectedWrong = new OpenProjectData();
      dataProtectedWrong.setProjectName("testprojectProtectedW");
      dataProtectedWrong.setProjectKey("testprojectProtectedW");
      dataProtectedWrong.setProjectAdminGroup("testgroupW");
      dataProtectedWrong.setSpecialPermissionSet(true);

      // group upper lower case
      OpenProjectData dataProtectedCase = new OpenProjectData();
      dataProtectedCase.setProjectName("testprojectProtectedC");
      dataProtectedCase.setProjectKey("F_KEY");
      dataProtectedCase.setProjectAdminGroup("testGroup");
      dataProtectedCase.setSpecialPermissionSet(true);

      Map<String, OpenProjectData> projects = new HashMap<>();
      projects.put(data.getProjectKey(), data);
      projects.put(dataProtected.getProjectKey(), dataProtected);
      projects.put(dataProtectedWrong.getProjectKey(), dataProtectedWrong);
      projects.put(dataProtectedCase.getProjectKey(), dataProtectedCase);

      Mockito.when(storage.listProjectHistory()).thenReturn(projects);
      adapter.setStorage(storage);

      SecurityContextHolder.getContext().setAuthentication(new TestAuthentication());

      Map<String, OpenProjectData> testresult = adapter.listProjectHistory();
      assertEquals(3, testresult.size());
      assertEquals(
          Lists.newArrayList("A_KEY", "F_KEY", "Z_KEY"), new ArrayList<>(testresult.keySet()));
      assertFalse(testresult.containsKey(dataProtectedWrong.getProjectKey()));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  public void getProjects() {

    adapter.setStorage(storage);

    Map<String, OpenProjectData> projects = new HashMap<>();

    OpenProjectData data = new OpenProjectData();
    data.setProjectName("testproject");
    data.setProjectKey("Z_KEY");
    data.setProjectAdminGroup("testgroup");

    projects.put(data.getProjectKey(), data);

    when(storage.listProjectHistory()).thenReturn(projects);

    Map<String, OpenProjectData> result = adapter.getProjects();

    assertEquals(projects.size(), result.size());
    assertEquals(data, result.get(data.getProjectKey()));
  }
}
