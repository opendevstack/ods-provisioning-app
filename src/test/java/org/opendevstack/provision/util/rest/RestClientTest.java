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

package org.opendevstack.provision.util.rest;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.util.CredentialsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Torsten Jaeschke
 * @author Stefan Lack
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringBoot.class)
@DirtiesContext
@WithMockUser(
    username = "testUser",
    roles = {"ADMIN"},
    password = "testUser")
@ActiveProfiles("crowd")
public class RestClientTest {

  @Value("${local.server.port}")
  private int randomServerPort;

  private RestClient client;

  @Value("${crowd.sso.cookie.name}")
  private String crowdSSOCookieName;

  @Autowired CrowdAuthenticationManager manager;

  @Before
  public void setUp() {
    client = new RestClient();
    client.setConnectTimeout(1);
    client.setReadTimeout(1);
    client.afterPropertiesSet();
  }

  @Test
  public void getClient() {
    Assert.assertThat(client.client, instanceOf(OkHttpClient.class));
  }

  @Test
  public void callHttpGreen() throws Exception {
    RestClientCall call = validGetCall();
    String response = client.execute(call);

    assertNotNull(response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void callHttpMissingUrl() throws Exception {
    client.execute(validGetCall().url(null));
  }

  @Test(expected = NullPointerException.class)
  public void callAuthWithoutCredentials() throws Exception {
    client.execute(validGetCall().credentials(null));
  }

  @Test
  public void callRealClientWrongPort() {
    try {
      RestClientCall invalidCall =
          validGetCall().url(String.format("http://localhost:%d", randomServerPort + 1));
      client.execute(invalidCall);
    } catch (SocketTimeoutException expectedInLocalEnv) {
    } catch (ConnectException expectedInJenkins) {
    } catch (IOException unexpected) {
      fail(unexpected.getMessage());
    }
  }

  public RestClientCall validGetCall() {
    return RestClientCall.get()
        .basicAuthenticated(new CredentialsInfo("testUser", "testUser"))
        .url(String.format("http://localhost:%d", randomServerPort))
        .mediaType(MediaType.parse("application/xhtml+xml"))
        .returnType(String.class);
  }
}
