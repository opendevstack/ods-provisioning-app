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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

/**
 * In line with https://docs.atlassian.com/jira/REST/server/#api/2/project-getProject to allow use
 * of given jira classes from Jira Rest Client lib but still let us create a new project, we need to
 * expand the already existing one with properties that they left out
 *
 * @author Pascal Brokmeier
 */
// otherwise jackson adds all the nulls and we don't want those in our call to
// the API
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeanJiraProject {

  @JsonProperty("projectTemplateKey")
  public String projectTemplateKey; // e.g. com.pyxis.greenhopper.jira:gh-scrum-template

  @JsonProperty("projectTypeKey")
  public String projectTypeKey; // eg business or software

  @JsonProperty("key")
  public String key;

  @JsonProperty("name")
  public String name;

  @JsonProperty("description")
  public String description;

  @JsonProperty("notificationScheme")
  public String notificationScheme; // the notificationSchemeId

  @JsonProperty("permissionScheme")
  private String permissionScheme;

  @JsonProperty("self")
  public URI self;

  public LeanJiraProject() {}

  public LeanJiraProject(
      URI self,
      String key,
      String name,
      String description,
      String projectTemplateKey,
      String projectTypeKey,
      String notificationSchemeId,
      String permissionSchemeId) {
    this.projectTemplateKey = projectTemplateKey;
    this.projectTypeKey = projectTypeKey;
    this.notificationScheme = notificationSchemeId;
    this.permissionScheme = permissionSchemeId;
    this.key = key;
    this.name = name;
    this.description = description;
    this.self = self;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
  public boolean hasPermissionSchemeId() {
    return permissionScheme != null;
  }

  @JsonIgnore
  public String getPermissionScheme() {
    return permissionScheme;
  }

}
