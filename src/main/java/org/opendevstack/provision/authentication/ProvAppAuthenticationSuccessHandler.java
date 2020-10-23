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

package org.opendevstack.provision.authentication;

import java.io.IOException;
import java.util.function.Function;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.Assert;

/** @author Sebastian Titakis */
public class ProvAppAuthenticationSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(ProvAppAuthenticationSuccessHandler.class);

  private final Function<Authentication, String> usernameProvider;

  public ProvAppAuthenticationSuccessHandler(Function<Authentication, String> usernameProvider) {
    Assert.notNull(usernameProvider, "UsernameProvider is null!");
    this.usernameProvider = usernameProvider;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws ServletException, IOException {

    super.onAuthenticationSuccess(request, response, authentication);

    try {
      String username = usernameProvider.apply(authentication);
      logger.info("Successful authentication [username=" + username + "]");

    } catch (Exception ex) {
      logger.debug("Error trying to resolve username of expired session!", ex);
    }
  }
}
