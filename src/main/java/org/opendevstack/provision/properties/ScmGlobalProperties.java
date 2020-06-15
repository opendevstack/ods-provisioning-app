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

package org.opendevstack.provision.properties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for global SCM settings
 *
 * @author tjaeschke
 */
@Configuration
@ConfigurationProperties("scm.global")
public class ScmGlobalProperties {

  private Map<String, List<String>> readableRepos;

  public Map<String, List<String>> getReadableRepos() {
    return readableRepos;
  }

  // readablerepos is a map of comma separated lists, such as:
  // scm.global.readablerepos.foo=bar,baz
  public void setReadableRepos(Map<String, String> readableRepos) {
    this.readableRepos =
        readableRepos.entrySet().stream()
            .collect(
                Collectors.toMap(
                    r -> r.getKey(), r -> Arrays.asList(r.getValue().trim().split("\\s*,\\s*"))));
  }
}
