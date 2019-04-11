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

package org.opendevstack.provision.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class LocalStorageTest {

  private LocalStorage localStorage;

  private static final Logger logger = LoggerFactory.getLogger(LocalStorageTest.class);

  @Before
  public void setUp() {
    localStorage = new LocalStorage();
    localStorage.setLocalStoragePath("src/test/resources/");
  }

  @Test(expected = IOException.class)
  public void storeProjectNoKey() throws Exception {
    ProjectData project = new ProjectData();
    localStorage.storeProject(project);
  }

  @Test
  public void storeProject() throws Exception {
    ProjectData project = new ProjectData();
    project.key = "clemens";
    String filePath = localStorage.storeProject(project);

    assertNotNull(filePath);

    project = localStorage.getProject(project.key);

    assertNotNull(project);
    assertTrue(project.jiraconfluencespace);
    assertEquals("clemens", project.key);

    // try the new field
    project.jiraconfluencespace = false;
    localStorage.updateStoredProject(project);
    project = localStorage.getProject(project.key);

    assertNotNull(project);
    assertFalse(project.jiraconfluencespace);

    (new File(filePath)).delete();
  }

  @Test(expected = IOException.class)
  public void storeProjectWithException() throws Exception {
    ProjectData project = new ProjectData();
    localStorage.setLocalStoragePath("/to/some/non/existant/folder/");
    localStorage.storeProject(project);
  }

  @Test
  public void listProjectHistory() throws Exception {
    Map<String, ProjectData> history = localStorage.listProjectHistory();

    assertFalse(history.isEmpty());
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

    data.aboutDataList = new ArrayList<AboutChangesData.AboutRecordData>();
    data.aboutDataList.add(single);
    data.aboutDataList.add(single2);

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
    assertNotNull(data.aboutDataList);
    assertTrue(data.aboutDataList.size() > 0);

    AboutChangesData.AboutRecordData single = data.aboutDataList.get(0);

    assertEquals("clemens", single.who);
  }

  @Test
  public void getProjectWithNull() throws Exception {
     assertNull(localStorage.getProject(null));
  }

}