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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.http.HttpAuthenticator;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationToken;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DirtiesContext
public class SSOAuthProcessingFilterTest {

  @MockBean private HttpAuthenticator httpAuthenticator;

  @MockBean private HttpServletRequest request;

  @MockBean private HttpServletResponse response;

  @MockBean private SSOAuthProcessingFilterBasicAuthStrategy basicAuthStrategy;

  private SSOAuthProcessingFilter filter = new SSOAuthProcessingFilter();

  @Before
  public void setup() {
    filter.setBasicAuthHandlerStrategy(basicAuthStrategy);
    filter.setHttpAuthenticator(httpAuthenticator);
  }

  @Test
  public void storeCrowdToken() {
    CrowdSSOAuthenticationToken token = new CrowdSSOAuthenticationToken("token");

    assertTrue(filter.storeTokenIfCrowd(request, response, token));
    assertFalse(filter.storeTokenIfCrowd(request, response, null));
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
