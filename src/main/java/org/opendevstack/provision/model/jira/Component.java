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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

/*
 * { "name": "Component 1", "description": "This is a JIRA component", "leadUserName": "fred",
 * "assigneeType": "PROJECT_LEAD", "isAssigneeTypeValid": false, "project": "PROJECTKEY",
 * "projectId": 10000 }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "name",
  "description",
  "leadUserName",
  "assigneeType",
  "isAssigneeTypeValid",
  "project",
  "projectId"
})
@Generated(value = {"JSON-to-Pojo-Generator"})
public class Component {

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("leadUserName")
  private String leadUserName;

  @JsonProperty("assigneeType")
  private String assigneeType;

  @JsonProperty("isAssigneeTypeValid")
  private Boolean isAssigneeTypeValid;

  @JsonProperty("project")
  private String project;

  @JsonProperty("projectId")
  private Integer projectId;

  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("leadUserName")
  public String getLeadUserName() {
    return leadUserName;
  }

  @JsonProperty("leadUserName")
  public void setLeadUserName(String leadUserName) {
    this.leadUserName = leadUserName;
  }

  @JsonProperty("assigneeType")
  public String getAssigneeType() {
    return assigneeType;
  }

  @JsonProperty("assigneeType")
  public void setAssigneeType(String assigneeType) {
    this.assigneeType = assigneeType;
  }

  @JsonProperty("isAssigneeTypeValid")
  public Boolean getIsAssigneeTypeValid() {
    return isAssigneeTypeValid;
  }

  @JsonProperty("isAssigneeTypeValid")
  public void setIsAssigneeTypeValid(Boolean isAssigneeTypeValid) {
    this.isAssigneeTypeValid = isAssigneeTypeValid;
  }

  @JsonProperty("project")
  public String getProject() {
    return project;
  }

  @JsonProperty("project")
  public void setProject(String project) {
    this.project = project;
  }

  @JsonProperty("projectId")
  public Integer getProjectId() {
    return projectId;
  }

  @JsonProperty("projectId")
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
