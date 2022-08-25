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

import static java.util.stream.Collectors.toSet;

import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import java.util.List;
import java.util.Set;

public class OpenshiftClient {

  private final OpenShiftClient osClient;

  private final String url;

  public OpenshiftClient(String url) {
    this(url, new DefaultOpenShiftClient(url));
  }

  public OpenshiftClient(String url, OpenShiftClient ocClient) {
    this.url = url;
    this.osClient = ocClient;
  }

  public Set<String> projects() {

    List<Project> projects = osClient.projects().list().getItems();
    return projects.stream().map(p -> p.getMetadata().getName()).collect(toSet());
  }

  public String getUrl() {
    return url;
  }
}
