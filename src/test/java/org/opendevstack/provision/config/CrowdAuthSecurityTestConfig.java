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
package org.opendevstack.provision.config;

import static org.opendevstack.provision.config.AuthSecurityTestConfig.TEST_VALID_CREDENTIAL;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@Profile("utestcrowd")
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "utestcrowd")
public class CrowdAuthSecurityTestConfig {

  @Bean
  public AuthenticationProvider authenticationProvider(
      @Qualifier("testUsersAndRoles") @Autowired Map<String, String> testUsersAndRoles) {
    return new TestingAuthenticationProvider(testUsersAndRoles);
  }

  private class TestingAuthenticationProvider
      extends org.springframework.security.authentication.TestingAuthenticationProvider {

    private Map<String, String> users;

    public TestingAuthenticationProvider(Map<String, String> testUsersAndRoles) {
      this.users = testUsersAndRoles;
    }

    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {

      if (null == authentication || null == authentication.getPrincipal()) {
        return null;
      } else {
        CrowdUserDetails user = (CrowdUserDetails) authentication.getPrincipal();
        String username = user.getUsername();
        if (!users.containsKey(username)) {
          return null;
        }

        if (!TEST_VALID_CREDENTIAL.equals(authentication.getCredentials())) {
          return null;
        }

        return new UsernamePasswordAuthenticationToken(
            authentication.getPrincipal(),
            authentication.getCredentials(),
            List.of((GrantedAuthority) () -> users.get(username)));
      }
    }

    public boolean supports(Class<?> authentication) {
      return true;
    }
  }
}
