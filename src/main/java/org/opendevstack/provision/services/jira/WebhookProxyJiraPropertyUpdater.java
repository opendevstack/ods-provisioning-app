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
import java.net.URL;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.webhookproxy.WebhookProxyUrlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebhookProxyJiraPropertyUpdater {

  private static final Logger logger =
      LoggerFactory.getLogger(WebhookProxyJiraPropertyUpdater.class);

  public static final String WEBHOOK_PROXY_URL_ENDPOINT_TEMPLATE_CONFIG_KEY_PREFIX =
      "jira.project.template.webhook-proxy-url-endpoint-template.";

  public static final String WEBHOOK_PROXY_URL_PAYLOAD_TEMPLATE_CONFIG_KEY_PREFIX =
      "jira.project.template.webhook-proxy-url-payload-template.";

  public static final String WEBHOOK_PROXY_SECRET_ENDPOINT_TEMPLATE_CONFIG_KEY_PREFIX =
      "jira.project.template.webhook-proxy-secret-endpoint-template.";

  public static final String WEBHOOK_PROXY_SECRET_PAYLOAD_TEMPLATE_CONFIG_KEY_PREFIX =
      "jira.project.template.webhook-proxy-secret-payload-template.";

  @Autowired private JiraProjectPropertyUpdater jiraProjectPropertyUpdater;

  @Autowired private WebhookProxyUrlFactory webhookProxyUrlFactory;

  public void addWebhookProxyProperty(
      JiraRestService restService, String projectKey, String projectType) throws IOException {

    URL webhookProxyUrl = webhookProxyUrlFactory.createBuildUrl(projectKey);

    logger.info("Adding webhook proxy property to jira project [projectKey={}]", projectKey);

    jiraProjectPropertyUpdater.setPropertyInJiraProject(
        restService,
        projectKey,
        projectType,
        webhookProxyUrl.toString(),
        WEBHOOK_PROXY_URL_ENDPOINT_TEMPLATE_CONFIG_KEY_PREFIX,
        WEBHOOK_PROXY_URL_PAYLOAD_TEMPLATE_CONFIG_KEY_PREFIX);
  }

  public void addWebhookSecretProperty(JiraRestService restService, OpenProjectData project)
      throws IOException {
    String projectKey = project.getProjectKey();
    String projectType = project.getProjectType();

    logger.info("Adding webhook proxy secret property to jira project [projectKey={}]", projectKey);

    jiraProjectPropertyUpdater.setPropertyInJiraProject(
        restService,
        projectKey,
        projectType,
        project.getWebhookProxyHmacKey(),
        WEBHOOK_PROXY_SECRET_ENDPOINT_TEMPLATE_CONFIG_KEY_PREFIX,
        WEBHOOK_PROXY_SECRET_PAYLOAD_TEMPLATE_CONFIG_KEY_PREFIX);
  }
}
