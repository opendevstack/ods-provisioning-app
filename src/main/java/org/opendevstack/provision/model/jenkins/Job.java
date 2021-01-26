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
import java.util.Optional;
import javax.annotation.Generated;
import org.opendevstack.provision.config.Quickstarter;

@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

  private String id;
  private boolean enabled;
  private String name;
  private String description;
  private String gitRepoName;
  private String jenkinsfilePath;
  private String branch;

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
    this.gitRepoName = gitRepoName;
    this.jenkinsfilePath = jenkinsfilePath;
    this.branch = branch;
  }

  public Job(Quickstarter qs, String odsGitRef) {
    this(qs.getName(), qs.getRepo(), qs.getBranch(), qs.getJenkinsfile(), odsGitRef);
    description = qs.getDesc();
  }

  public Job(
      String jobname,
      String repo,
      Optional<String> branch,
      Optional<String> jenkinsfile,
      String odsGitRef) {
    int gitExtenstionIndex = repo.lastIndexOf(".git");
    if (gitExtenstionIndex == -1) {
      gitRepoName = repo;
    } else {
      gitRepoName = repo.substring(0, gitExtenstionIndex);
    }
    jenkinsfilePath = jenkinsfile.orElse(jobname + "/Jenkinsfile");
    this.branch = branch.orElse(odsGitRef);
    enabled = true;
    id = jobname;
    name = jobname;
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

  public String getGitRepoName() {
    return gitRepoName;
  }

  public String getJenkinsfilePath() {
    return jenkinsfilePath;
  }

  public String getBranch() {
    return branch;
  }

  public void setGitRepoName(String gitRepoName) {
    this.gitRepoName = gitRepoName;
  }

  public void setJenkinsfilePath(String jenkinsfilePath) {
    this.jenkinsfilePath = jenkinsfilePath;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  @Override
  public String toString() {
    return "Job{"
        + "id="
        + id
        + ", enabled="
        + enabled
        + ", name="
        + name
        + ", description="
        + description
        + ", gitRepoName="
        + gitRepoName
        + ", jenkinsfilePath="
        + jenkinsfilePath
        + ", branch="
        + branch
        + '}';
  }
}
