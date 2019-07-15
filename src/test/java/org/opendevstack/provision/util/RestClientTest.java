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

package org.opendevstack.provision.util;

import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringBoot.class)
@DirtiesContext
@WithMockUser(username = "testUser", roles = {"ADMIN"}, password = "testUser")
public class RestClientTest {

  private static final Logger logger = LoggerFactory.getLogger(RestClientTest.class);

  @Value("${local.server.port}")
  private int randomServerPort;

  private RestClient client;

  @Value("${crowd.sso.cookie.name}")
  private String crowdSSOCookieName;

  @Autowired
  CustomAuthenticationManager manager;

  @Autowired
  RestClient realClient;

  @Before
  public void setUp() {
    client = new RestClient();
    client.setConnectTimeout(1);
    client.setReadTimeout(1);
    CrowdCookieJar cookieJar = new CrowdCookieJar();
    cookieJar.setDomain("localhost");
    cookieJar.setSSOCookieName(crowdSSOCookieName);
    client.setCookieJar(cookieJar);
  }

  @Test
  public void getClient() throws Exception {
    assertTrue(client.getClient(null) instanceof OkHttpClient);
  }

  @Test
  public void getClientWithCrowdCookie() throws Exception {
    assertTrue(client.getClient("test") instanceof OkHttpClient);
  }

  @Test
  public void getSessionId() throws Exception {
    client.getSessionId(String.format("http://localhost:%d", randomServerPort));
  }

  @Test(expected = IOException.class)
  public void getSessionIdWithException() throws Exception {
    client.getSessionId(String.format("http://invalid_address", randomServerPort));
  }

  @Test
  public void callHttpGreen() throws Exception {
    String response = client.callHttp(String.format("http://localhost:%d", randomServerPort),
        "ClemensTest", false, HTTP_VERB.GET, String.class);

    assertNotNull(response);

    response = client.callHttp(String.format("http://localhost:%d", randomServerPort),
        "ClemensTest", false, HTTP_VERB.POST, String.class);

    assertNotNull(response);
  }

  @Test(expected = NullPointerException.class)
  public void callHttpMissingVerb() throws Exception {
    client.callHttp(String.format("http://localhost:%d", randomServerPort), "ClemensTest", false,
        null, String.class);
  }

  @Test(expected = NullPointerException.class)
  public void callHttpMissingUrl() throws Exception {
    client.callHttp(null, "ClemensTest", false, null, String.class);
  }

  @Test(expected = NullPointerException.class)
  public void callAuthWithoutCredentials() throws Exception {
    client.callHttpBasicFormAuthenticate(String.format("http://localhost:%d", randomServerPort));
  }

  @Test(expected = NullPointerException.class)
  public void callAuthWithoutUrl() throws Exception {
    client.callHttpBasicFormAuthenticate(null);
  }

  @Test
  public void callRealClientWrongPort() {
    RestClient spyAdapter = Mockito.spy(client);

    try {
      spyAdapter.callHttp(String.format("http://localhost:%d", 1000), "ClemensTest", false,
          HTTP_VERB.GET, String.class);
    } catch (SocketTimeoutException se) {
      // expected in local env
    } catch (ConnectException ce) {
      // expected in jenkins
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

}
