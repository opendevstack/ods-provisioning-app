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

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.webhookproxy.WebhookProxyUrlFactory;

@ExtendWith(MockitoExtension.class)
public class WebhookProxyJiraPropertyUpdaterTest {

  @InjectMocks private WebhookProxyJiraPropertyUpdater webhookProxyJiraPropertyUpdater;

  @Mock private JiraProjectPropertyUpdater jiraProjectPropertyUpdater;

  @Mock private JiraAdapter jiraRestService;

  @Mock private WebhookProxyUrlFactory webhookProxyUrlFactory;

  @Test
  public void givenAddWebhookProxy_whenEndpointIsNotValidUrl_thenIOException() {

    try {
      doThrow(new IOException("URL not valid"))
          .when(jiraProjectPropertyUpdater)
          .setPropertyInJiraProject(
              any(JiraRestService.class),
              anyString(),
              anyString(),
              anyString(),
              anyString(),
              anyString());

      //      URL url = mock(URL.class);
      //      when(url.toString()).thenReturn("http")

      when(webhookProxyUrlFactory.createBuildUrl("empty", "empty"))
          .thenReturn(new URL("https://ods.lan"));

      webhookProxyJiraPropertyUpdater.addWebhookProxyProperty(
          jiraRestService, "empty", "empty", "empty");

      fail();
    } catch (IOException e) {
      e.getMessage().contains("URL not valid");
    }
  }

  @Test
  public void givenAddWebhookProxy_whenAllParametersAreOk_thenCallJiraPropertyUpdater()
      throws IOException {

    String projectKey = "testPropect";
    String projectType = "utest-project-template";
    String webhookSecret = "secret";

    URL webhookProxyUrl = new URL("https://ods.lan?secret=" + webhookSecret);

    when(webhookProxyUrlFactory.createBuildUrl(projectKey, webhookSecret))
        .thenReturn(webhookProxyUrl);

    webhookProxyJiraPropertyUpdater.addWebhookProxyProperty(
        jiraRestService, projectKey, projectType, webhookSecret);

    verify(jiraProjectPropertyUpdater, times(1))
        .setPropertyInJiraProject(
            jiraRestService,
            projectKey,
            projectType,
            webhookProxyUrl.toString(),
            WebhookProxyJiraPropertyUpdater.WEBHOOK_PROXY_URL_ENDPOINT_TEMPLATE_CONFIG_KEY_PREFIX,
            WebhookProxyJiraPropertyUpdater.WEBHOOK_PROXY_URL_PAYLOAD_TEMPLATE_CONFIG_KEY_PREFIX);
  }
}
