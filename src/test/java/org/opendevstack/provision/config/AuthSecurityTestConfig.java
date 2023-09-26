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

import static java.util.Map.entry;
import static org.mockito.Mockito.mock;

import java.util.Map;
import net.sf.ehcache.CacheManager;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthSecurityTestConfig {

  public static final String TEST_USER_USERNAME = "user";
  public static final String TEST_ADMIN_USERNAME = "admin";
  public static final String TEST_ADMIN_EMAIL = "admin@example.com";
  public static final String TEST_NOT_PERMISSIONED_USER_USERNAME = "not-permissioned-user";
  public static final String TEST_VALID_CREDENTIAL = "validsecret";

  @Value("${idmanager.group.opendevstack-users}")
  protected String roleUser;

  @Value("${idmanager.group.opendevstack-administrators}")
  protected String roleAdmin;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean(name = "testUsersAndRoles")
  public Map<String, String> testUsersAndRoles() {
    return Map.ofEntries(
        entry(TEST_USER_USERNAME, roleUser),
        entry(TEST_ADMIN_USERNAME, roleAdmin),
        entry(TEST_NOT_PERMISSIONED_USER_USERNAME, "not-opendevstack-user"));
  }

  @Bean
  @Primary
  public IODSAuthnzAdapter iodsAuthnzAdapter() {
    return mock(IODSAuthnzAdapter.class);
  }

}
