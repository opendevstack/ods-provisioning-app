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

package org.opendevstack.provision.model.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

/**
 * In line with https://docs.atlassian.com/jira/REST/server/#api/2/project-getProject to allow use
 * of given jira classes from Jira Rest Client lib but still let us create a new project, we need to
 * expand the already existing one with properties that they left out
 */
// otherwise jackson adds all the nulls and we don't want those in our call to
// the API
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullJiraProject extends LeanJiraProject {

  @JsonProperty("lead")
  private String lead;

  public FullJiraProject() {
    super(null, null, null, null, null, null, null, null);
  }

  public FullJiraProject(
      URI self,
      String key,
      String name,
      String description,
      String lead,
      String projectTemplateKey,
      String projectTypeKey,
      String notificationSchemeId,
      String permissionSchemeId) {
    super(
        self,
        key,
        name,
        description,
        projectTemplateKey,
        projectTypeKey,
        notificationSchemeId,
        permissionSchemeId);
    this.lead = lead;
  }

  public void setLead(String lead) {
    this.lead = lead;
  }
}
