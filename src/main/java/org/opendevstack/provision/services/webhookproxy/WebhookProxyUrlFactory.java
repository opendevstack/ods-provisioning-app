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
package org.opendevstack.provision.services.webhookproxy;

import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookProxyUrlFactory {

  private static final Logger logger = LoggerFactory.getLogger(WebhookProxyUrlFactory.class);

  public static final String BUILD_ENDPOINT = "/build";
  public static final String WEBHOOK_PROXY_PROTOCOL = "https";
  public static final String TRIGGER_SECRET_PARAMETER = "trigger_secret";

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
  private String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  public URL createBuildUrl(String projectKey, String webhookProxySecret)
      throws MalformedURLException {

    String resolvedUrl =
        String.format(
            WEBHOOK_PROXY_PROTOCOL
                + "://"
                + projectOpenshiftJenkinsWebhookProxyNamePattern
                + BUILD_ENDPOINT
                + "?"
                + TRIGGER_SECRET_PARAMETER
                + "=",
            projectKey,
            projectOpenshiftBaseDomain);

    logger.info(
        "Resolved webhook proxy url based on template [projectKey={}, urlTemplate={}, resolvedUrl={}]!",
        projectKey,
        projectOpenshiftJenkinsWebhookProxyNamePattern,
        resolvedUrl + "**************");

    // adding now secret to resolved url to avoid leaking the secret in the log file
    return new URL(resolvedUrl + webhookProxySecret);
  }
}
