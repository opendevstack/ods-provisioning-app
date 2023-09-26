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

package org.opendevstack.provision.authentication.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.http.CrowdHttpAuthenticator;
import com.atlassian.crowd.integration.http.util.CrowdHttpTokenHelper;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationToken;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.CrowdClient;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class SSOAuthProcessingFilterTest {

  @MockBean private CrowdHttpAuthenticator crowdHttpAuthenticator;

  @MockBean private HttpServletRequest request;

  @MockBean private HttpServletResponse response;

  @MockBean private SSOAuthProcessingFilterBasicAuthStrategy basicAuthStrategy;

  @MockBean private CrowdClient crowdClient;

  @MockBean private CrowdHttpTokenHelper crowdHttpTokenHelper;

  @MockBean private ClientProperties clientProperties;

  private SSOAuthProcessingFilter filter;

  @BeforeEach
  public void setup() {
    // TODO instantiate params
    filter = new SSOAuthProcessingFilter(crowdHttpTokenHelper, crowdClient, clientProperties);
    filter.setBasicAuthHandlerStrategy(basicAuthStrategy);
    filter.setHttpAuthenticator(crowdHttpAuthenticator);
  }

  @Test
  public void storeCrowdToken() {
    CrowdSSOAuthenticationToken token = new CrowdSSOAuthenticationToken("token");

    assertTrue(filter.storeTokenIfCrowdMethodUsed(request, response, token));
    assertFalse(filter.storeTokenIfCrowdMethodUsed(request, response, null));
  }

  @Test
  public void testSuccessfullAuth() throws Exception {
    CrowdSSOAuthenticationToken token = new CrowdSSOAuthenticationToken("token");

    filter.successfulAuthentication(request, response, null, token);
  }

  @Test
  public void whenRequiresAuthentication_callsBasicAuthStrategy() {

    // case basic auth authentication exists
    when(basicAuthStrategy.requiresAuthentication(
            any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(Boolean.FALSE);

    assertFalse(filter.requiresAuthentication(request, response));
  }
}
