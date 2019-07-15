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

package org.opendevstack.provision.model;

import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.opendevstack.provision.model.bitbucket.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ProjectData
 *
 * @author Torsten Jaeschke
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectData {
  public List<BasicUser> admins = new ArrayList<>();
  public String name = null;
  public String description = null;
  public String key = null;
  public String jiraId = null;
  public List<Map<String, String>> quickstart = null;
  public boolean jiraconfluencespace = true;
  public boolean openshiftproject = true;
  public String jiraUrl = null;
  public String confluenceUrl = null;
  public String bitbucketUrl = null;
  public Map<String, Map<String, List<Link>>> repositories = null;
  public String openshiftJenkinsUrl = null;
  public String openshiftConsoleDevEnvUrl = null;
  public String openshiftConsoleTestEnvUrl = null;

  // permissions
  public String adminGroup = null;
  public String userGroup = null;
  public String admin = null;
  public String readonlyGroup = null;

  public boolean createpermissionset = false;

  @JsonIgnoreProperties({"lastJobs"})
  public List<String> lastJobs = null;

  public String projectType = null;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ProjectData other = (ProjectData) obj;

    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    return true;
  }
}
