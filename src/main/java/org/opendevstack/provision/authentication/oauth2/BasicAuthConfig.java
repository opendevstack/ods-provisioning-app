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

import com.atlassian.crowd.integration.http.CrowdHttpAuthenticator;
import com.atlassian.crowd.integration.http.CrowdHttpAuthenticatorImpl;

import com.atlassian.crowd.integration.http.util.CrowdHttpTokenHelperImpl;
import com.atlassian.crowd.integration.http.util.CrowdHttpValidationFactorExtractorImpl;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.integration.springsecurity.RemoteCrowdAuthenticationProvider;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsServiceImpl;

import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import net.sf.ehcache.CacheManager;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@Configuration
@ConditionalOnExpression(
    "'${provision.auth.basic-auth.enabled}'=='true' && '${provision.auth.provider}'=='oauth2'")
public class BasicAuthConfig {

  private static final Logger logger = LoggerFactory.getLogger(BasicAuthConfig.class);

  @Value("${idmanager.realm:provision}")
  private String idManagerRealm;

  @Value("${crowd.application.name}")
  String crowdApplicationName;

  @Value("${crowd.application.password}")
  String crowdApplicationPassword;

  @Value("${crowd.server.url}")
  String crowdServerUrl;

  @Value("${crowd.cookie.domain}")
  String cookieDomain;

  @Bean
  public RemoteCrowdAuthenticationProvider crowdAuthenticationProvider() throws IOException {
    logger.info(
        "Created RemoteCrowdAuthenticationProvider to enable REST API calls with Basic Auth beside OAuth2!");
    return new RemoteCrowdAuthenticationProvider(
            crowdClient(), httpAuthenticator(), crowdUserDetailsService());
  }

  @Bean
  public CrowdAuthenticationManager simpleCrowdAuthenticationManager() throws IOException {
    return new CrowdAuthenticationManager(crowdClient());
  }

  @Bean
  public CrowdClient crowdClient() throws IOException {
    return new RestCrowdClientFactory().newInstance(getProps());
  }

  @Bean
  public CrowdHttpAuthenticator httpAuthenticator() throws IOException {
    return new CrowdHttpAuthenticatorImpl(
            crowdClient(),
            getProps(),
            // TODO
            CrowdHttpTokenHelperImpl.getInstance(CrowdHttpValidationFactorExtractorImpl.getInstance()));
  }

  public ClientProperties getProps() throws IOException {

    Properties prop = new Properties();
    try (InputStream in =
                 Thread.currentThread().getContextClassLoader().getResourceAsStream("crowd.properties")) {
      prop.load(in);
    }
    prop.setProperty("application.name", crowdApplicationName);
    prop.setProperty("application.password", crowdApplicationPassword);
    prop.setProperty("crowd.server.url", crowdServerUrl);
    prop.setProperty("cookie.domain", cookieDomain);

    return ClientPropertiesImpl.newInstanceFromProperties(prop);
  }

  @Bean
  public CrowdUserDetailsService crowdUserDetailsService() throws IOException {
    CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
   /*
    cusd.setUserManager(userManager());
    cusd.setGroupMembershipManager(
        new ProvAppSimpleCachingGroupMembershipManager(
            securityServerClient(), userManager(), groupManager(), getCache(), true));*/
    cusd.setAuthorityPrefix("");
    return cusd;
  }
/*
  @Bean
  public UserManager userManager() throws IOException {
    return new CachingUserManager(securityServerClient(), getCache());
  }

  @Bean
  public BasicCache getCache() {
    return new CacheImpl(getCacheManager());
  }*/

  @Bean
  public CacheManager getCacheManager() {
    return getEhCacheFactory().getObject();
  }

  @Bean
  public EhCacheManagerFactoryBean getEhCacheFactory() {
    EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
    factoryBean.setConfigLocation(new ClassPathResource("crowd-ehcache.xml"));
    return factoryBean;
  }
/*
  @Bean
  public GroupManager groupManager() throws IOException {
    return new CachingGroupManager(securityServerClient(), getCache());
  }*/

  @Bean
  public BasicAuthenticationEntryPoint basicAuthEntryPoint() {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(idManagerRealm);
    return entryPoint;
  }
}
