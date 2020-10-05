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
package org.opendevstack.provision.services.openshift;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IResource;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Sebastian Titakis */
@RunWith(SpringRunner.class)
public class OpenshiftClientTest {

  @MockBean private IClient ocClient;

  private String url = "http://url.com";

  private OpenshiftClient openshiftClient;

  @Before
  public void setup() {
    openshiftClient = new OpenshiftClient(url, ocClient);
  }

  @Test
  public void testOpenshiftClientReturnsProjectKeys() {

    String projectname = "ods";
    IResource resource = mock(IResource.class);
    when(resource.getName()).thenReturn(projectname);
    List.of(resource);

    when(ocClient.list("Project")).thenReturn(List.of(resource));

    Set<String> projects = openshiftClient.projects();
    assertEquals(1, projects.size());
    assertTrue(projects.contains(projectname));
  }

  @Test
  public void testGetUrl() {
    assertEquals(url, openshiftClient.getUrl());
  }
}
