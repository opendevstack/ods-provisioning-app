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

package org.opendevstack.provision.services.jira;

import com.google.common.base.Preconditions;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JiraProjectTypePropertyCalculator {

  @Autowired private Environment environment;

  @Qualifier("projectTemplateKeyNames")
  @Autowired
  private List<String> projectTemplateKeyNames;

  public String readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
      String projectType, String templatePrefix, String defaultValue) {
    Preconditions.checkNotNull(templatePrefix, "no template prefix passed");
    Preconditions.checkNotNull(defaultValue, "no defaultValue passed");
    /*
     * if the type can be found in the global definition of types (projectTemplateKeyNames) and is
     * also configured for jira (environment.containsProperty) - take it, if not fall back to
     * default
     */
    return (projectType != null
            && environment.containsProperty(templatePrefix + projectType)
            && projectTemplateKeyNames.contains(projectType))
        ? environment.getProperty(templatePrefix + projectType)
        : defaultValue;
  }

  public boolean isPropertyAvailable(String projectType, String templatePrefix) {
    return environment.containsProperty(templatePrefix + projectType);
  }

  public String readProperty(String projectType, String templatePrefix) {
    return environment.getProperty(templatePrefix + projectType);
  }
}
