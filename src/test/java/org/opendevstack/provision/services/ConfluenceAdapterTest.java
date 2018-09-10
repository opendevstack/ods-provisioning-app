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

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.doReturn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.SpaceData;
import org.opendevstack.provision.model.confluence.Space;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ConfluenceAdapterTest {

  @Autowired
  @InjectMocks
  ConfluenceAdapter confluenceAdapter;

  @Test
  public void createConfluenceSpaceForProject() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    ProjectData project = Mockito.mock(ProjectData.class);
    SpaceData spaceData = Mockito.mock(SpaceData.class);
    Space space = Mockito.mock(Space.class);

    // Mockito.when(spyAdapter.callCreateSpaceApi(space, "crowdCookieValue").thenReturn(spaceData));
    doReturn(space).when(spyAdapter).createSpaceData(project);
    doReturn(spaceData).when(spyAdapter).callCreateSpaceApi(Matchers.any(Space.class),
        Matchers.anyString());
    Mockito.when(spaceData.getUrl()).thenReturn("testUrl");

    ProjectData createdProject = spyAdapter.createConfluenceSpaceForProject(project, "crowdCookieValue");
    
    assertEquals("testUrl", createdProject.confluenceUrl);
  }

  @Test
  public void callCreateSpaceApi() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    Space space = new Space();
    SpaceData expectedSpaceData = Mockito.mock(SpaceData.class);

    doReturn(expectedSpaceData).when(spyAdapter).post(Matchers.anyString(), Matchers.anyString(),
        Matchers.anyString(), Matchers.any(SpaceData.class.getClass()));
    
    SpaceData createdSpaceData = spyAdapter.callCreateSpaceApi(space, "crowdCookieValue");

    assertEquals(expectedSpaceData, createdSpaceData);
  }
}
