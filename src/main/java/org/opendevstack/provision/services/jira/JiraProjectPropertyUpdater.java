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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** @author Sebastian Titakis, Georg Federmann */
@Component
public class JiraProjectPropertyUpdater {

  private static final Logger logger = LoggerFactory.getLogger(JiraProjectPropertyUpdater.class);

  public static final Map<String, String> HTTP_HEADERS_CONTENT_TYPE_JSON_AND_ACCEPT_JSON =
      Collections.unmodifiableMap(
          Map.of("Content-Type", "application/json", "Accept", "application/json"));
  public static final String JIRA_PROJECT_KEY = "%PROJECT_KEY%";
  public static final String JIRA_PROJECT_PROPERTY_VALUE = "%PROPERTY_VALUE%";

  @Value("${jira.uri}")
  private String jiraUri;

  @Autowired private Environment environment;

  public void setPropertyInJiraProject(
      JiraRestService restService,
      String projectKey,
      String projectType,
      String projectPropertyValue,
      String jiraTemplateProjectPropertyEndpointTemplatePrefix,
      String jiraTemplateProjectPropertyPayloadTemplatePrefix)
      throws IOException {

    String endpointTemplate =
        environment.getProperty(jiraTemplateProjectPropertyEndpointTemplatePrefix + projectType);

    if (null == endpointTemplate) {
      String message =
          String.format(
              "No jira endpoint is defined to set the jira project property [projectKey=%s, projectType=%s]",
              projectKey, projectType);
      throw new RuntimeException(message);
    }

    String payloadTemplate =
        environment.getProperty(jiraTemplateProjectPropertyPayloadTemplatePrefix + projectType);

    if (null == payloadTemplate) {
      String message =
          String.format(
              "No jira endpoint payload is defined to set the jira project property [projectKey=%s, projectType=%s]",
              projectKey, projectType);
      throw new RuntimeException(message);
    }

    endpointTemplate = jiraUri + endpointTemplate;

    String endpoint =
        JiraProjectPropertyUpdater.parseTemplate(
            projectKey, projectPropertyValue, endpointTemplate);
    logger.debug(
        "Resolved endpoint [projectKey={}, projectType={}, endpoint={}]",
        projectKey,
        projectType,
        endpoint);

    String payload =
        JiraProjectPropertyUpdater.parseTemplate(projectKey, projectPropertyValue, payloadTemplate);
    logger.debug(
        "Resolved endpointPayload [projectKey={}, projectType={}, payload={}]",
        projectKey,
        projectType,
        payload);

    RestClientCall clientCall =
        restService
            .httpPost()
            .url(endpoint)
            .body(payload)
            .header(HTTP_HEADERS_CONTENT_TYPE_JSON_AND_ACCEPT_JSON);
    restService.getRestClient().execute(clientCall);
  }

  public static String parseTemplate(
      String projectKey, String projectPropertyValue, String template) {
    String endpoint = template.replace(JIRA_PROJECT_KEY, projectKey);
    endpoint = endpoint.replace(JIRA_PROJECT_PROPERTY_VALUE, projectPropertyValue);
    return endpoint;
  }
}
