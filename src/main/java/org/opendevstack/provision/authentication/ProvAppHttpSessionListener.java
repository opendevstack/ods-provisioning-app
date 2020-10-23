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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Function;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.Assert;

/** @author Sebastian Titakis */
public class ProvAppHttpSessionListener implements HttpSessionListener {

  private static final Logger logger = LoggerFactory.getLogger(ProvAppHttpSessionListener.class);

  public static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

  private final Function<Authentication, String> usernameProvider;

  public ProvAppHttpSessionListener(Function<Authentication, String> usernameProvider) {
    Assert.notNull(usernameProvider, "UsernameProvider is null!");
    this.usernameProvider = usernameProvider;
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {

    try {
      LocalDateTime creationTime =
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(se.getSession().getCreationTime()), ZoneId.systemDefault());

      LocalDateTime lastAccessedTime =
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(se.getSession().getLastAccessedTime()), ZoneId.systemDefault());

      SecurityContextImpl securityContext =
          (SecurityContextImpl) se.getSession().getAttribute(SPRING_SECURITY_CONTEXT);
      Authentication authentication = securityContext.getAuthentication();
      String username = usernameProvider.apply(authentication);

      logger.info(
          "Session destroyed [id={}, username={}, creationTime={}, lastAccessedTime={}, maxInterval={} secs]",
          se.getSession().getId(),
          username,
          creationTime,
          lastAccessedTime,
          se.getSession().getMaxInactiveInterval());

    } catch (Exception ex) {
      logger.debug("Error trying to log session expired details!", ex.getMessage());
    }
  }
}
