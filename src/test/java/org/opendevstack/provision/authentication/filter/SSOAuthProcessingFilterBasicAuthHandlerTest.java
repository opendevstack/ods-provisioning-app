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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DirtiesContext
public class SSOAuthProcessingFilterBasicAuthHandlerTest {

  @MockBean private HttpServletRequest request;

  @MockBean private HttpServletResponse response;

  @Test
  public void requiresAuthentication() {

    // if basic auth is not enabled always return true
    SSOAuthProcessingFilterBasicAuthHandler handler =
        new SSOAuthProcessingFilterBasicAuthHandler(false);
    assertTrue(handler.requiresAuthentication(request, response));

    // Request has to include basic auth header
    when(request.getHeader("Authorization")).thenReturn("Basic adfafadfdsfdsf");

    // if basic auth is enabled and no authentication object exists in security context returns true
    handler = new SSOAuthProcessingFilterBasicAuthHandler(true);
    assertTrue(handler.requiresAuthentication(request, response));

    TestAuthentication testAuthentication = new TestAuthentication();
    TestSecurityContextHolder.getContext().setAuthentication(testAuthentication);

    // if basic auth is enabled and authentication object exists in security context but is not
    // authenticated return true
    handler = new SSOAuthProcessingFilterBasicAuthHandler(true);
    assertTrue(handler.requiresAuthentication(request, response));

    // if basic auth is enabled and no authentication object exists in security context but is
    // authenticated false
    handler = new SSOAuthProcessingFilterBasicAuthHandler(true);
    testAuthentication.setAuthenticated(true);
    assertFalse(handler.requiresAuthentication(request, response));
  }
}
