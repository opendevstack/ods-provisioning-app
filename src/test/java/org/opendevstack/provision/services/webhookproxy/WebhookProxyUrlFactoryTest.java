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

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = SpringBoot.class)
@ActiveProfiles("utest")
public class WebhookProxyUrlFactoryTest {

  @Value("${openshift.jenkins.project.webhookproxy.host.pattern}")
  private String projectOpenshiftJenkinsWebhookProxyNamePattern;

  @Value("${openshift.apps.basedomain}")
  private String projectOpenshiftBaseDomain;

  @Autowired private WebhookProxyUrlFactory webhookProxyUrlFactory;

  @Test
  public void givenCreateBuildUrl_whenParamsAreValid_thenReturnValidUrl()
      throws MalformedURLException {

    String projectKey = "key";
    String secret = "secret";

    URL url = webhookProxyUrlFactory.createBuildUrl(projectKey, secret);

    String domain =
        String.format(
            projectOpenshiftJenkinsWebhookProxyNamePattern, projectKey, projectOpenshiftBaseDomain);

    assertEquals(WebhookProxyUrlFactory.WEBHOOK_PROXY_PROTOCOL, url.getProtocol().toString());
    assertEquals(domain, url.getHost());
    assertEquals(WebhookProxyUrlFactory.BUILD_ENDPOINT, url.getPath());
    assertEquals(WebhookProxyUrlFactory.TRIGGER_SECRET_PARAMETER + "=" + secret, url.getQuery());
  }
}
