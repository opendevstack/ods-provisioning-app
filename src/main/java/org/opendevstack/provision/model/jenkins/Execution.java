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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties({"options"})
public class Execution {

  private List<Option> env = new LinkedList<>();
  private String branch;
  private String repository;
  private String project;
  private String url;

  public void setOptions(Map<String, String> options) {
    options.keySet().stream().forEach(x -> env.add(new Option(x, options.get(x))));
  }

  public String getOptionValue(String name) {
    return env.stream()
        .filter(o -> o.getName().equals(name))
        .findFirst()
        .map(Option::getValue)
        .orElse("");
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("env", env)
        .append("branch", branch)
        .append("repository", repository)
        .append("project", project)
        .append("url", url)
        .toString();
  }

  public List<Option> getEnv() {
    return env;
  }

  public void setEnv(List<Option> env) {
    this.env = env;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
