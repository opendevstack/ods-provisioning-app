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

import com.atlassian.crowd.integration.http.CrowdHttpAuthenticator;
import com.atlassian.crowd.integration.http.util.CrowdHttpTokenHelper;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationProcessingFilter;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationToken;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.CrowdClient;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/** Custom processing filter to enable SSO via Crowd for the provision app */
public class SSOAuthProcessingFilter extends CrowdSSOAuthenticationProcessingFilter {

  private static final Logger logger = LoggerFactory.getLogger(SSOAuthProcessingFilter.class);

  private CrowdHttpAuthenticator crowdHttpAuthenticator;

  private SSOAuthProcessingFilterBasicAuthStrategy basicAuthHandlerStrategy;

  public SSOAuthProcessingFilter(
      CrowdHttpTokenHelper tokenHelper,
      CrowdClient crowdClient,
      ClientProperties clientProperties) {
    super(tokenHelper, crowdClient, clientProperties);
  }

  /**
   * Method to handle a successful authentication
   *
   * @param request
   * @param response
   * @param chain
   * @param authResult
   * @throws IOException
   * @throws ServletException
   */
  @Override
  protected void successfulAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      Authentication authResult)
      throws IOException, ServletException {
    storeTokenIfCrowdMethodUsed(request, response, authResult);
    logger.debug("AuthResult {}", authResult.getCredentials().toString());
    super.successfulAuthentication(request, response, chain, authResult);
  }

  /**
   * Set the HttpAuthenticator, which handles the authentication via SOAP
   *
   * @param httpAuthenticator
   */
  public void setHttpAuthenticator(CrowdHttpAuthenticator httpAuthenticator) {
    this.crowdHttpAuthenticator = httpAuthenticator;
    super.setHttpAuthenticator(httpAuthenticator);
  }

  /**
   * If the authentication has been done via crowd, a cookie is written, because crowd uses the
   * cookie to authenticate
   *
   * @param request
   * @param response
   * @param authResult
   */
  boolean storeTokenIfCrowdMethodUsed(
      HttpServletRequest request, HttpServletResponse response, Authentication authResult) {
    if (authResult instanceof CrowdSSOAuthenticationToken && authResult.getCredentials() != null) {
      try {
        // TODO check if this suffices
        super.storeTokenIfCrowd(request, response, authResult);
        return true;
      } catch (Exception e) {
        logger.error("Unable to set Crowd SSO token", e);
      }
    }
    return false;
  }

  public CrowdHttpAuthenticator getAuthenticator() {
    return crowdHttpAuthenticator;
  }

  public void setBasicAuthHandlerStrategy(
      SSOAuthProcessingFilterBasicAuthStrategy ssoFilterBasicAuthHandlerStrategy) {
    this.basicAuthHandlerStrategy = ssoFilterBasicAuthHandlerStrategy;
  }

  @Override
  protected boolean requiresAuthentication(
      HttpServletRequest request, HttpServletResponse response) {

    // For basic auth requires authentication should be skipped
    // if security context already has an authentication object
    if (!basicAuthHandlerStrategy.requiresAuthentication(request, response)) {
      return false;
    }

    return super.requiresAuthentication(request, response);
  }
}
