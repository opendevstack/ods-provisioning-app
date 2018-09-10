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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendevstack.provision.model.bitbucket.Link;
import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ProjectData
 *
 * @author Torsten Jaeschke
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectData {
  public List<BasicUser> admins = new ArrayList<>();
  public String name = null;
  public String description = null;
  public String key = null;
  public String jiraId = null;
  public List<String> tags = new ArrayList<>();
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
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((admins == null) ? 0 : admins.hashCode());
    result = prime * result + ((bitbucketUrl == null) ? 0 : bitbucketUrl.hashCode());
    result = prime * result + ((confluenceUrl == null) ? 0 : confluenceUrl.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((jiraId == null) ? 0 : jiraId.hashCode());
    result = prime * result + ((jiraUrl == null) ? 0 : jiraUrl.hashCode());
    result = prime * result + (jiraconfluencespace ? 1231 : 1237);
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result
        + ((openshiftConsoleDevEnvUrl == null) ? 0 : openshiftConsoleDevEnvUrl.hashCode());
    result = prime * result
        + ((openshiftConsoleTestEnvUrl == null) ? 0 : openshiftConsoleTestEnvUrl.hashCode());
    result = prime * result + ((openshiftJenkinsUrl == null) ? 0 : openshiftJenkinsUrl.hashCode());
    result = prime * result + ((quickstart == null) ? 0 : quickstart.hashCode());
    result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
    result = prime * result + ((tags == null) ? 0 : tags.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ProjectData other = (ProjectData) obj;
    if (admins == null) {
      if (other.admins != null)
        return false;
    } else if (!admins.equals(other.admins))
      return false;
    if (bitbucketUrl == null) {
      if (other.bitbucketUrl != null)
        return false;
    } else if (!bitbucketUrl.equals(other.bitbucketUrl))
      return false;
    if (confluenceUrl == null) {
      if (other.confluenceUrl != null)
        return false;
    } else if (!confluenceUrl.equals(other.confluenceUrl))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (jiraId == null) {
      if (other.jiraId != null)
        return false;
    } else if (!jiraId.equals(other.jiraId))
      return false;
    if (jiraUrl == null) {
      if (other.jiraUrl != null)
        return false;
    } else if (!jiraUrl.equals(other.jiraUrl))
      return false;
    if (jiraconfluencespace != other.jiraconfluencespace)
      return false;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (openshiftConsoleDevEnvUrl == null) {
      if (other.openshiftConsoleDevEnvUrl != null)
        return false;
    } else if (!openshiftConsoleDevEnvUrl.equals(other.openshiftConsoleDevEnvUrl))
      return false;
    if (openshiftConsoleTestEnvUrl == null) {
      if (other.openshiftConsoleTestEnvUrl != null)
        return false;
    } else if (!openshiftConsoleTestEnvUrl.equals(other.openshiftConsoleTestEnvUrl))
      return false;
    if (openshiftJenkinsUrl == null) {
      if (other.openshiftJenkinsUrl != null)
        return false;
    } else if (!openshiftJenkinsUrl.equals(other.openshiftJenkinsUrl))
      return false;
    if (quickstart == null) {
      if (other.quickstart != null)
        return false;
    } else if (!quickstart.equals(other.quickstart))
      return false;
    if (repositories == null) {
      if (other.repositories != null)
        return false;
    } else if (!repositories.equals(other.repositories))
      return false;
    if (tags == null) {
      if (other.tags != null)
        return false;
    } else if (!tags.equals(other.tags))
      return false;
    return true;
  }



}
