/*
 * Copyright 2018 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import okhttp3.OkHttpClient;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringBoot.class)
@DirtiesContext
public class RestClientTest {

  @LocalServerPort
  int randomServerPort;

  private RestClient client;

  @Before
  public void setUp() {
    client = new RestClient();
    client.setConnectTimeout(1);
    client.setReadTimeout(1);
    CrowdCookieJar cookieJar = new CrowdCookieJar();
    cookieJar.setDomain("localhost");
    client.setCookieJar(cookieJar);
  }


  @Test
  public void getClient() throws Exception {
    assertTrue(client.getClient() instanceof OkHttpClient);
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
}
