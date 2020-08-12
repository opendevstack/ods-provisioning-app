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
package org.opendevstack.provision.authentication.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Configuration of BasicAuth for testing purposes.
 *
 * <p>Never move it to src/main/java folder!
 *
 * @author Sebastian Titakis
 */
@Configuration
@ConditionalOnProperty(name = "provision.auth.basic-auth.enabled", havingValue = "utest")
public class BasicAuthSecurityTestConfig {

  public static final String TEST_USER_USERNAME = "user";
  public static final String TEST_USER_EMAIL = "user@example.com";
  public static final String TEST_ADMIN_USERNAME = "admin";
  public static final String TEST_ADMIN_EMAIL = "admin@example.com";
  public static final String TEST_NOT_PERMISSIONED_USER_USERNAME = "not-permissioned-user";
  public static final String TEST_NOT_PERMISSIONED_USER_EMAIL = "not-permissioned-user@example.com";
  public static final String TEST_REALM = "test-realm";
  public static final String TEST_VALID_CREDENTIAL = "validsecret";

  @Value("${idmanager.group.opendevstack-users}")
  private String roleUser;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String roleAdmin;

  @Bean
  public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(TEST_REALM);
    return entryPoint;
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
      @Qualifier("testUsersAndRoles") @Autowired Map<String, String> testUsersAndRoles) {
    TestingAuthenticationProvider provider = new TestingAuthenticationProvider(testUsersAndRoles);
    return provider;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean(name = "testUsersAndRoles")
  public Map<String, String> testUsersAndRoles() {
    Map<String, String> users = new HashMap<>();
    users.put(TEST_USER_USERNAME, roleUser);
    users.put(TEST_ADMIN_USERNAME, roleAdmin);
    users.put(TEST_NOT_PERMISSIONED_USER_USERNAME, "not-opendevstack-user");
    return users;
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
        String username = (String) authentication.getPrincipal();
        if (!users.containsKey(username)) {
          return null;
        }

        if (!TEST_VALID_CREDENTIAL.equals(authentication.getCredentials())) {
          return null;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(
                authentication.getPrincipal(),
                authentication.getCredentials(),
                List.of(
                    new GrantedAuthority() {
                      @Override
                      public String getAuthority() {
                        return users.get(username);
                      }
                    }));
        return authenticationToken;
      }
    }

    public boolean supports(Class<?> authentication) {
      return true;
    }
  }
}
