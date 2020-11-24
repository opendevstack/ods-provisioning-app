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

package org.opendevstack.provision.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.opendevstack.provision.services.jira.JiraProjectTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jira")
@Configuration
@Validated
public class JiraProjectTemplateProperties {

  private static final Logger logger = LoggerFactory.getLogger(JiraProjectTemplateProperties.class);

  private final Map<String, JiraProjectTemplate> projectTemplates = new HashMap<>();

  private ObjectMapper mapper = new ObjectMapper();

  public Map<String, JiraProjectTemplate> getProjectTemplates() {
    return projectTemplates;
  }

  @PostConstruct
  public void logInitialization() throws JsonProcessingException {
    logger.info(
        "Project Templates loaded! [count={}, projectTemplates={}]",
        projectTemplates.keySet().size(),
        mapper.writeValueAsString(projectTemplates));
  }
}
