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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.SpaceData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Mock
  RestClient client;

  @Autowired
  @InjectMocks
  ConfluenceAdapter confluenceAdapter;
  
  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;
  
  @Test
  public void createConfluenceSpaceForProject() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    ProjectData project = Mockito.mock(ProjectData.class);
    SpaceData spaceData = Mockito.mock(SpaceData.class);
    Space space = Mockito.mock(Space.class);

    doReturn(space).when(spyAdapter).createSpaceData(project);
    doReturn(spaceData).when(spyAdapter).callCreateSpaceApi(any(Space.class),
        anyString());
    when(spaceData.getUrl()).thenReturn("testUrl");

    ProjectData createdProject = spyAdapter.createConfluenceSpaceForProject(project, "crowdCookieValue");
    
    assertEquals("testUrl", createdProject.confluenceUrl);
  }

  @Test
  public void callCreateSpaceApi() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    spyAdapter.client = client;
    Space space = new Space();
    SpaceData expectedSpaceData = Mockito.mock(SpaceData.class);

    doReturn(expectedSpaceData).when(client).callHttp(anyString(), any(),
        anyString(), anyBoolean(),
        eq(RestClient.HTTP_VERB.POST), eq(SpaceData.class));
    
    SpaceData createdSpaceData = spyAdapter.callCreateSpaceApi(space, "crowdCookieValue");

    assertEquals(expectedSpaceData, createdSpaceData);
  }
  
  @Test
  public void updateSpacePermissions() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    ProjectData project = JiraAdapterTests.getTestProject("name");
    project.adminGroup = "adminGroup";
    project.userGroup = "userGroup";
    project.readonlyGroup = "readGroup";
    
    spyAdapter.client = client;

    doReturn("").when(client).callHttpString(anyString(), any(), anyString(), anyBoolean(), eq(RestClient.HTTP_VERB.POST));
    
    int permissionSets = spyAdapter.updateSpacePermissions(project, "crowdCookieValue");

    // 3 permission sets
    Mockito.verify(client, Mockito.times(1)).callHttpString
		(anyString(), contains(project.adminGroup), anyString(),
			anyBoolean(),
	        eq(RestClient.HTTP_VERB.POST));
    
    Mockito.verify(client, Mockito.times(1)).callHttpString
		(anyString(), contains(project.userGroup), anyString(),
			anyBoolean(),
	        eq(RestClient.HTTP_VERB.POST));

    Mockito.verify(client, Mockito.times(1)).callHttpString
		(anyString(), contains(project.readonlyGroup), anyString(),
			anyBoolean(),
	        eq(RestClient.HTTP_VERB.POST));
    
    assertEquals(3, permissionSets);
  }
  
  @Test
  public void testCreateSpaceData() throws Exception {
    ConfluenceAdapter spyAdapter = Mockito.spy(confluenceAdapter);
    ProjectData project = JiraAdapterTests.getTestProject("name");
    project.adminGroup = "adminGroup";
    project.userGroup = "adminGroup";
    project.readonlyGroup = "adminGroup";

    List blList = new ArrayList<>();
    	Blueprint bPrint = new Blueprint();
    	bPrint.setBlueprintModuleCompleteKey(confluenceBlueprintKey);
    	bPrint.setContentBlueprintId("1234");
    blList.add(bPrint);
    
    Mockito.doReturn(blList).when(spyAdapter).getList(contains("space-blueprint"), eq(null), any());

    Mockito.doReturn(new ArrayList<>()).when(spyAdapter).getList(contains("jira"), eq(null), any());

    Space space = spyAdapter.createSpaceData(project);

    assertNotNull(space);
    assertEquals(project.name, space.getName());
    assertEquals(project.key, space.getSpaceKey());

    assertNotNull(space.getContext());

    assertEquals(project.name, space.getContext().getProjectName());
    assertEquals(project.key, space.getContext().getProjectKey());

  }
  
}
