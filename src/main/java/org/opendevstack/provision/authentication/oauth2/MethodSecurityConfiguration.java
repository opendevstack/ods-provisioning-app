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

package org.opendevstack.provision.authentication.oauth2;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

/** @author Sebastian Titakis */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class MethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {

  @Value("${idmanager.group.opendevstack-users}")
  private String groupNameOpendevstackUser;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String groupNameOpendevstackAdministrator;

  @Override
  protected ProvAppExpressionHandler createExpressionHandler() {
    return new ProvAppExpressionHandler(
        groupNameOpendevstackUser.toLowerCase(), groupNameOpendevstackAdministrator.toLowerCase());
  }

  @Bean(name = "opendevstackRoles")
  public List<String> opendevstackRoles() {
    return Collections.unmodifiableList(
        List.of(
            groupNameOpendevstackUser.toLowerCase(),
            groupNameOpendevstackAdministrator.toLowerCase()));
  }
}
