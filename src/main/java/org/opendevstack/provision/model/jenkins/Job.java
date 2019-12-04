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

package org.opendevstack.provision.model.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Generated;
import org.opendevstack.provision.config.Quickstarter;

/** @author Torsten Jaeschke */
@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
  public String id;
  public String legacyCt;
  public boolean enabled;
  public String name;
  public String description;

  public String gitParentProject;
  public String gitRepoName;
  public String jenkinsfilePath;
  public String branch;

  public Job() {}

  public Job(
      String id,
      boolean enabled,
      String name,
      String description,
      String gitParentProject,
      String gitRepoName,
      String jenkinsfilePath,
      String branch) {
    this.id = id;
    this.enabled = enabled;
    this.name = name;
    this.description = description;
    this.gitParentProject = gitParentProject;
    this.gitRepoName = gitRepoName;
    this.jenkinsfilePath = jenkinsfilePath;
    this.branch = branch;
  }

  public Job(Quickstarter quickstarter) {
    this(quickstarter.getName(), quickstarter.getUrl(), quickstarter.getLegacyCt());
    description = quickstarter.getDesc();
  }

  public Job(String jobname, String url, String legacyCt) {
    String gitURL = url.split("\\.git")[0];
    gitParentProject = gitURL.split("/")[0];
    gitRepoName = gitURL.split("/")[1];
    jenkinsfilePath = url.split("\\.git")[1];
    branch = "master";
    if (jenkinsfilePath.startsWith("#")) {
      Pattern pattern = Pattern.compile("#([a-zA-Z]*)\\/(.*)");
      Matcher matcher = pattern.matcher(jenkinsfilePath);
      matcher.find();
      branch = matcher.group(1);
      jenkinsfilePath = matcher.group(2);
    } else {
      jenkinsfilePath = jenkinsfilePath.substring(1);
    }
    enabled = true;
    id = jobname;
    name = jobname;
    this.legacyCt = legacyCt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String toFormattedString() {
    return String.format("Job id: %s, name: %s", id, name);
  }

  public String getGitParentProject() {
    return gitParentProject;
  }

  public String getGitRepoName() {
    return gitRepoName;
  }

  public String getJenkinsfilePath() {
    return jenkinsfilePath;
  }

  public String getBranch() {
    return branch;
  }

  public String getLegacyCt() {
    return legacyCt;
  }

  @Override
  public String toString() {
    return "Job{"
        + "id="
        + id
        + ", legacyCt="
        + legacyCt
        + ", enabled="
        + enabled
        + ", name="
        + name
        + ", description="
        + description
        + ", gitParentProject="
        + gitParentProject
        + ", gitRepoName="
        + gitRepoName
        + ", jenkinsfilePath="
        + jenkinsfilePath
        + ", branch="
        + branch
        + '}';
  }
}
