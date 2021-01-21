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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IResource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpenshiftClientTest {

  @InjectMocks private OpenshiftClient openshiftClient;

  @Mock private IClient ocClient;

  private String url = "http://url.com";

  @BeforeEach
  public void setup() {
    openshiftClient = new OpenshiftClient(url, ocClient);
  }

  @Test
  public void testOpenshiftClientReturnsProjectKeys() {

    String projectName = "ods";
    IResource resource = mock(IResource.class);
    when(resource.getName()).thenReturn(projectName);
    List.of(resource);

    when(ocClient.list("Project")).thenReturn(List.of(resource));

    Set<String> projects = openshiftClient.projects();
    assertEquals(1, projects.size());
    assertTrue(projects.contains(projectName));
  }

  @Test
  public void testGetUrl() {
    assertEquals(url, openshiftClient.getUrl());
  }
}
