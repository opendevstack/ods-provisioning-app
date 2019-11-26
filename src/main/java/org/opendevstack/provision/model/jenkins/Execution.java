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

/** @author Torsten Jaeschke */
@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties({"options"})
public class Execution {

  public List<Option> env = new LinkedList<>();
  public String branch;
  public String repository;
  public String project;
  public String url;

  public void setOptions(Map<String, String> options) {
    options.keySet().stream().forEach(x -> env.add(new Option(x, options.get(x))));
  }

  public String getOptionValue(String name) {
    return env.stream()
        .filter(o -> o.name.equals(name))
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
}
