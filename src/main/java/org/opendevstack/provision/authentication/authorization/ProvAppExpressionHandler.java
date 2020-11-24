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
package org.opendevstack.provision.authentication.authorization;

import java.util.Collections;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

/** Provides custom expression-based access control */
public class ProvAppExpressionHandler extends DefaultMethodSecurityExpressionHandler {

  private static Logger logger = LoggerFactory.getLogger(ProvAppExpressionHandler.class);

  public static final String PROV_APP = "provApp";

  private final String userRole;

  private final String adminRole;

  private final List<String> roles;

  public ProvAppExpressionHandler(String userRole, String adminRole) {
    this.userRole = userRole;
    this.adminRole = adminRole;
    roles = Collections.unmodifiableList(List.of(adminRole, userRole));
    setExpressionParser(new SpelExpressionParser());
  }

  @Override
  public StandardEvaluationContext createEvaluationContextInternal(
      Authentication authentication, MethodInvocation mi) {
    StandardEvaluationContext ec = super.createEvaluationContextInternal(authentication, mi);
    ec.setVariable(PROV_APP, this);
    logger.debug("Registered '{}' as expression-based keyword", PROV_APP);
    return ec;
  }

  public String adminRole() {
    return adminRole;
  }

  public String userRole() {
    return userRole;
  }

  public List<String> allRoles() {
    return roles;
  }
}
