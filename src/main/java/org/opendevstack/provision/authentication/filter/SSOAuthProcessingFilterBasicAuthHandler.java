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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** @author Sebastian Titakis */
public class SSOAuthProcessingFilterBasicAuthHandler
    implements SSOAuthProcessingFilterBasicAuthStrategy {

  private boolean isBasicAuthEnabled;

  public SSOAuthProcessingFilterBasicAuthHandler(boolean isBasicAuthEnabled) {
    this.isBasicAuthEnabled = isBasicAuthEnabled;
  }

  @Override
  public boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {

    // For basic auth requires authentication should be skipped
    // if security context already has an authentication object
    if (isBasicAuthEnabled) {
      String authorization = request.getHeader("Authorization");
      if (authorization != null && authorization.startsWith("Basic")) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (null != authentication && authentication.isAuthenticated()) {
          return false;
        }
      }
    }

    return true;
  }
}
