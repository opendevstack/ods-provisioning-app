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

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IResource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenshiftClient {

  private final IClient iClient;

  private final String url;

  public OpenshiftClient(String url, String token) {
    this(url, new ClientBuilder(url).usingToken(token).build());
  }

  public OpenshiftClient(String url, IClient ocClient) {
    this.url = url;
    this.iClient = ocClient;
  }

  public Set<String> projects() {

    List<IResource> projectResource = iClient.list("Project");

    return projectResource.stream().map(project -> project.getName()).collect(Collectors.toSet());
  }

  public String getUrl() {
    return url;
  }
}
