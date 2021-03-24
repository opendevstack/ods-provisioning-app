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

package org.opendevstack.provision.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.adapter.ISCMAdapter.URL_TYPE;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.OpenProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

public class LocalStorageTest {

  private LocalStorage localStorage;

  private static final Logger logger = LoggerFactory.getLogger(LocalStorageTest.class);

  @BeforeEach
  public void setUp() {
    localStorage = new LocalStorage();
    localStorage.setLocalStoragePath("src/test/resources/");
  }

  @Test
  public void storeProjectNoKey() {
    assertThrows(
        IOException.class,
        () -> {
          OpenProjectData project = new OpenProjectData();
          localStorage.storeProject(project);
        });
  }

  @Test
  public void upgradeExisting() throws Exception {
    File testFile = new File(localStorage.getLocalStoragePath() + "/20170101000000-test.txt");

    File testFileBackup =
        new File(localStorage.getLocalStoragePath() + "/20170101000000-test.txt.kup");

    FileCopyUtils.copy(new FileInputStream(testFile), new FileOutputStream(testFileBackup));
    try {
      OpenProjectData project = localStorage.getProject("Test");

      // try the new field
      project.setBugtrackerSpace(false);
      String currentPath = project.getPhysicalLocation();
      assertNotNull(currentPath);
      assertTrue(localStorage.updateStoredProject(project));
      project = localStorage.getProject(project.getProjectKey());

      assertNotNull(project);
      assertFalse(project.isBugtrackerSpace());

      Map<String, Map<URL_TYPE, String>> repositories = project.getRepositories();

      assertNotNull(repositories);
      Map<URL_TYPE, String> occonfigrepo = repositories.get("odsew-occonfig-artifacts");

      assertNotNull(occonfigrepo);

      assertEquals(3, occonfigrepo.size());

      assertEquals(currentPath, project.getPhysicalLocation());
    } catch (Exception allErr) {
      throw allErr;
    } finally {
      FileCopyUtils.copy(new FileInputStream(testFileBackup), new FileOutputStream(testFile));
      testFileBackup.deleteOnExit();
    }
  }

  @Test
  public void storeProject() throws Exception {
    OpenProjectData project = new OpenProjectData();
    project.setProjectKey("clemens");
    String filePath = localStorage.storeProject(project);
    assertNotNull(project.getPhysicalLocation());

    assertNotNull(filePath);

    project = localStorage.getProject(project.getProjectKey());

    assertNotNull(project);
    assertTrue(project.isBugtrackerSpace());
    assertEquals("clemens", project.getProjectKey());

    // try the new field
    project.setBugtrackerSpace(false);
    localStorage.updateStoredProject(project);
    project = localStorage.getProject(project.getProjectKey());

    assertNotNull(project);
    assertFalse(project.isBugtrackerSpace());
    assertNotNull(project.getPhysicalLocation());

    (new File(filePath)).delete();
  }

  @Test
  public void storeProjectWithException() {
    assertThrows(
        IOException.class,
        () -> {
          OpenProjectData project = new OpenProjectData();
          localStorage.setLocalStoragePath("/to/some/non/existant/folder/");
          localStorage.storeProject(project);
        });
  }

  @Test
  public void listProjectHistory() throws Exception {
    Map<String, OpenProjectData> history = localStorage.listProjectHistory();

    assertFalse(history.isEmpty());

    assertEquals("Test", history.values().iterator().next().getProjectName());
  }

  @Test
  public void writeAboutChangesData() throws Exception {
    AboutChangesData data = new AboutChangesData();

    AboutChangesData.AboutRecordData single = new AboutChangesData.AboutRecordData();

    single.who = "clemens";
    single.when = "2017-17-21";
    single.what = "test";

    AboutChangesData.AboutRecordData single2 = new AboutChangesData.AboutRecordData();

    single.who = "clemens";
    single.when = "2017-17-21";
    single.what = "test";

    data.setAboutDataList(new ArrayList<>());
    data.getAboutDataList().add(single);
    data.getAboutDataList().add(single2);

    String testWrite = localStorage.storeAboutChangesData(data);
    logger.debug("AboutChanges: " + testWrite);

    assertTrue(testWrite.contains(single.who));
    assertTrue(testWrite.contains(single.what));
    assertTrue(testWrite.contains(single.when));
  }

  @Test
  public void listAboutChangesData() throws Exception {
    AboutChangesData data = localStorage.listAboutChangesData();

    assertNotNull(data);
    assertNotNull(data.getAboutDataList());
    assertTrue(data.getAboutDataList().size() > 0);

    AboutChangesData.AboutRecordData single = data.getAboutDataList().get(0);

    assertEquals("clemens", single.who);
  }

  @Test
  public void getProjectWithNull() throws Exception {
    assertNull(localStorage.getProject(null));
  }
}
