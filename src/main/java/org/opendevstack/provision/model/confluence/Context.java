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

package org.opendevstack.provision.model.confluence;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Generated;

/** @author Torsten Jaeschke */
@Generated(value = {"JSON-to-Pojo-Generator"})
public class Context {

  /*
   * "context": { "jira-server":"06509280-5441-3f2e-b5d1-60fcc24a85aa", "jira-project":"10200",
   * "name":"fdsssdf", "spaceKey":"FDSSSDF", "description":"", "noPageTitlePrefix":"true",
   * "atl_token":"undefined", "jira-server-id":"06509280-5441-3f2e-b5d1-60fcc24a85aa",
   * "project-key":"FDSSS", "project-name":"fdsssdf", "ContentPageTitle":"fdsssdf" }
   */

  private String jiraServer = null;
  private String jiraProject = null;
  private String name = null;
  private String spaceKey = null;
  private String description = null;
  private String noPageTitlePrefix = null;
  private String atlToken = null;
  private String jiraServerId = null;
  private String projectKey = null;
  private String projectName = null;
  private String contentPageTitle = null;

  @JsonProperty("jira-server")
  public String getJiraServer() {
    return jiraServer;
  }

  public void setJiraServer(String jiraServer) {
    this.jiraServer = jiraServer;
  }

  @JsonProperty("jira-project")
  public String getJiraProject() {
    return jiraProject;
  }

  public void setJiraProject(String jiraProject) {
    this.jiraProject = jiraProject;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpaceKey() {
    return spaceKey;
  }

  public void setSpaceKey(String spaceKey) {
    this.spaceKey = spaceKey;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNoPageTitlePrefix() {
    return noPageTitlePrefix;
  }

  public void setNoPageTitlePrefix(String noPageTitlePrefix) {
    this.noPageTitlePrefix = noPageTitlePrefix;
  }

  @JsonProperty("atl_token")
  public String getAtlToken() {
    return atlToken;
  }

  public void setAtlToken(String atlToken) {
    this.atlToken = atlToken;
  }

  @JsonProperty("jira-server-id")
  public String getJiraServerId() {
    return jiraServerId;
  }

  public void setJiraServerId(String jiraServerId) {
    this.jiraServerId = jiraServerId;
  }

  @JsonProperty("project-key")
  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  @JsonProperty("project-name")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getContentPageTitle() {
    return contentPageTitle;
  }

  public void setContentPageTitle(String contentPageTitle) {
    this.contentPageTitle = contentPageTitle;
  }
}
