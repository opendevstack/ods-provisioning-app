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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.controller.ProjectAPI;
import org.opendevstack.provision.util.CredentialsInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles("utest")
public class RestClientTest {

  @Value("${local.server.port}")
  private int randomServerPort;

  private RestClient client;

  @BeforeEach
  public void setUp() {
    client = new RestClient();
    client.setConnectTimeout(5);
    client.setReadTimeout(25);
    client.afterPropertiesSet();
  }

  @Test
  public void getClient() {
    assertNotNull(client.getClient());
  }

  @Test
  public void callHttpNotAuthorized() {
    try {
      RestClientCall call = validGetCall("/" + ProjectAPI.API_V2_PROJECT);
      call.basicAuthenticated(new CredentialsInfo("unknow_user", "secret"));
      Object execute = client.execute(call);
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Errorcode: 401"));
    }
  }

  @Test
  public void callHttpMissingUrl() {
    assertThrows(NullPointerException.class, () -> client.execute(validGetCall().url(null)));
  }

  @Test
  public void callRealClientWrongPort() {
    try {
      RestClientCall invalidCall =
          validGetCall().url(String.format("http://localhost:%d", randomServerPort + 1));
      client.execute(invalidCall);
    } catch (SocketTimeoutException | ConnectException ignored) {
    } catch (IOException unexpected) {
      fail(unexpected.getMessage());
    }
  }

  public RestClientCall validGetCall() {
    return validGetCall("");
  }

  public RestClientCall validGetCall(String path) {
    return RestClientCall.get()
        .url(
            String.format(
                "http://localhost:%d%s",
                randomServerPort, path == null || path.isEmpty() ? "" : path))
        .mediaType(MediaType.parse("application/xhtml+xml"))
        .returnType(String.class);
  }
}
