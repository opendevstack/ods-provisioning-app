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

package org.opendevstack.provision.model.jira;

import com.atlassian.jira.rest.client.OptionalIterable;
import com.atlassian.jira.rest.client.domain.*;
import com.fasterxml.jackson.annotation.*;

import java.net.URI;
import java.util.Collection;

/**
 * In line with https://docs.atlassian.com/jira/REST/server/#api/2/project-getProject to allow use
 * of given jira classes from Jira Rest Client lib but still let us create a new project, we need to
 * expand the already existing one with properties that they left out
 *
 * @author Pascal Brokmeier
 */
// otherwise jackson adds all the nulls and we don't want those in our call to the API
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullJiraProject extends Project {

  public String projectTemplateKey; // e.g. com.pyxis.greenhopper.jira:gh-scrum-template
  public String projectTypeKey; // eg business or software
  public String id;
  public String permissionScheme; // the permissionSchemeId
  public String notificationScheme; // the notificationSchemeId

  public FullJiraProject() {
    super(null, null, null, null, null, null, null, null, null, null);
  }

  public FullJiraProject(
      URI self,
      String key,
      String name,
      String description,
      BasicUser lead,
      URI uri,
      Collection<Version> versions,
      Collection<BasicComponent> components,
      OptionalIterable<IssueType> issueTypes,
      Collection<BasicProjectRole> projectRoles,
      String projectTemplateKey,
      String projectTypeKey,
      String notificationSchemeId) {
    super(self, key, name, description, lead, uri, versions, components, issueTypes, projectRoles);
    this.projectTemplateKey = projectTemplateKey;
    this.projectTypeKey = projectTypeKey;
    this.notificationScheme = notificationSchemeId;
  }

  // needed because Jira API doesn't want the whole user but rather just the unique ID/name of the
  // user
  // https://stackoverflow.com/questions/17542240/how-to-serialize-only-the-id-of-a-child-with-jackson
  @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name")
  @JsonIdentityReference(alwaysAsId = true) // otherwise first ref as POJO, others as id
  public BasicUser getLead() {
    return super.getLead();
  }
}
