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

package org.opendevstack.provision.authentication.crowd;

import com.atlassian.crowd.integration.http.CrowdHttpAuthenticator;
import com.atlassian.crowd.integration.http.CrowdHttpAuthenticatorImpl;
import com.atlassian.crowd.integration.http.util.CrowdHttpTokenHelper;
import com.atlassian.crowd.integration.http.util.CrowdHttpTokenHelperImpl;
import com.atlassian.crowd.integration.http.util.CrowdHttpValidationFactorExtractor;
import com.atlassian.crowd.integration.http.util.CrowdHttpValidationFactorExtractorImpl;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.integration.springsecurity.CrowdLogoutHandler;
import com.atlassian.crowd.integration.springsecurity.RemoteCrowdAuthenticationProvider;
import com.atlassian.crowd.integration.springsecurity.UsernameStoringAuthenticationFailureHandler;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsServiceImpl;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionListener;
import org.jetbrains.annotations.NotNull;
import org.opendevstack.provision.authentication.ProvAppHttpSessionListener;
import org.opendevstack.provision.authentication.filter.SSOAuthProcessingFilter;
import org.opendevstack.provision.authentication.filter.SSOAuthProcessingFilterBasicAuthHandler;
import org.opendevstack.provision.authentication.filter.SSOAuthProcessingFilterBasicAuthStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableCaching
@EnableEncryptableProperties
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "crowd")
public class CrowdSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(CrowdSecurityConfiguration.class);

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

  @Value("${provision.auth.basic-auth.enabled:true}")
  private boolean isBasicAuthEnabled;

  @Value("${frontend.spa.enabled:false}")
  private boolean spafrontendEnabled;

  @Autowired(required = false)
  private BasicAuthenticationEntryPoint basicAuthEntryPoint;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    HttpSecurity sec =
        http.authenticationProvider(crowdAuthenticationProvider())
            .headers()
            .httpStrictTransportSecurity()
            .disable()
            .and()
            .cors()
            .disable()
            .csrf()
            .disable();

    if (isBasicAuthEnabled) {
      logger.info("Added Basic Auth entry point!");
      sec.httpBasic()
          .realmName(crowdApplicationName)
          .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
    }

    sec.addFilter(crowdSSOAuthenticationProcessingFilter())
        .authorizeRequests()
        .antMatchers(
            "/",
            "/fragments/**",
            "/webjars/**",
            "/js/**",
            "/json/**",
            "/favicon.ico",
            "/login",
            "/nfe/**")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and();

    sec.formLogin()
        .loginPage("/login")
        .permitAll()
        .and()
        .logout()
        .addLogoutHandler(logoutHandler())
        .permitAll()
        .and();
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
  //  @ConditionalOnProperty(name = "provision.auth.provider", havingValue = "crowd")
  public CrowdLogoutHandler logoutHandler() throws IOException {
    if (spafrontendEnabled) {
      OKResponseCrowdLogoutHandler handler = new OKResponseCrowdLogoutHandler();
      handler.setHttpAuthenticator(httpAuthenticator());
      return handler;

    } else {
      CrowdLogoutHandler clh = new CrowdLogoutHandler();
      clh.setHttpAuthenticator(httpAuthenticator());
      return clh;
    }
  }

  @Bean
  public SSOAuthProcessingFilter crowdSSOAuthenticationProcessingFilter() throws Exception {
    SSOAuthProcessingFilter filter =
        new SSOAuthProcessingFilter(
            CrowdHttpTokenHelperImpl.getInstance(
                CrowdHttpValidationFactorExtractorImpl.getInstance()),
            crowdClient(),
            getProps());
    filter.setBasicAuthHandlerStrategy(ssoFilterBasicAuthHandlerStrategy());
    filter.setHttpAuthenticator(httpAuthenticator());
    filter.setAuthenticationManager(authenticationManager());
    filter.setFilterProcessesUrl("/j_security_check");
    filter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
    filter.setAuthenticationFailureHandler(authenticationFailureHandler());
    filter.setUsernameParameter("username");
    filter.setPasswordParameter("password");
    return filter;
  }

  @Bean
  public SSOAuthProcessingFilterBasicAuthStrategy ssoFilterBasicAuthHandlerStrategy() {
    return new SSOAuthProcessingFilterBasicAuthHandler(isBasicAuthEnabled);
  }

  @Bean
  public AuthenticationSuccessHandler authenticationSuccessHandler() {
    if (spafrontendEnabled) {
      return createAuthSuccessHandlerThatReturnsOK();
    } else {
      return createAuthSuccessHandlerThatRedirectsToHome();
    }
  }

  @NotNull
  public SavedRequestAwareAuthenticationSuccessHandler
      createAuthSuccessHandlerThatRedirectsToHome() {
    SavedRequestAwareAuthenticationSuccessHandler successHandler =
        new SavedRequestAwareAuthenticationSuccessHandler() {

          @Override
          public void onAuthenticationSuccess(
              HttpServletRequest request,
              HttpServletResponse response,
              Authentication authentication)
              throws ServletException, IOException {

            super.onAuthenticationSuccess(request, response, authentication);

            CrowdSecurityConfiguration.logSuccessAuthentication(authentication);
          }
        };
    successHandler.setDefaultTargetUrl("/home");
    successHandler.setUseReferer(true);
    successHandler.setAlwaysUseDefaultTargetUrl(true);
    return successHandler;
  }

  @NotNull
  public SimpleUrlAuthenticationSuccessHandler createAuthSuccessHandlerThatReturnsOK() {
    SimpleUrlAuthenticationSuccessHandler handler =
        new SimpleUrlAuthenticationSuccessHandler() {

          @Override
          public void onAuthenticationSuccess(
              HttpServletRequest request,
              HttpServletResponse response,
              Authentication authentication) {

            clearAuthenticationAttributes(request);
            response.setStatus(HttpServletResponse.SC_OK);

            CrowdSecurityConfiguration.logSuccessAuthentication(authentication);
          }
        };
    return handler;
  }

  public static void logSuccessAuthentication(Authentication authentication) {
    try {
      if (authentication.getPrincipal() instanceof CrowdUserDetails) {
        CrowdUserDetails userDetails = (CrowdUserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        logger.info("Successful authentication [username=" + username + "]");
      }

    } catch (Exception ex) {
      logger.debug("Error trying to resolve username of expired session!", ex);
    }
  }

  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    UsernameStoringAuthenticationFailureHandler failureHandler =
        new UsernameStoringAuthenticationFailureHandler("TODO_USERNAME");
    failureHandler.setDefaultFailureUrl("/login?error=true");
    failureHandler.setUseForward(true);
    return failureHandler;
  }

  @Bean
  @ConditionalOnProperty(
      name = "provision.auth.provider",
      havingValue = "crowd",
      matchIfMissing = true)
  public CrowdClient crowdClient() throws IOException {
    return new RestCrowdClientFactory().newInstance(getProps());
  }

  @Bean
  public CrowdAuthenticationManager crowdAuthenticationManager() throws IOException {
    return new CrowdAuthenticationManager(crowdClient(), getCrowdServerUrl());
  }

  @Bean
  public CrowdHttpValidationFactorExtractor crowdHttpValidationFactorExtractor() {
    return CrowdHttpValidationFactorExtractorImpl.getInstance();
  }

  @Bean
  public CrowdHttpTokenHelper crowdHttpTokenHelper() {
    return CrowdHttpTokenHelperImpl.getInstance(crowdHttpValidationFactorExtractor());
  }

  @Bean
  public CrowdHttpAuthenticator httpAuthenticator() throws IOException {
    return new CrowdHttpAuthenticatorImpl(
        crowdClient(),
        getProps(),
        // TODO
        CrowdHttpTokenHelperImpl.getInstance(CrowdHttpValidationFactorExtractorImpl.getInstance()));
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(crowdAuthenticationProvider());
  }

  @Bean
  public CrowdUserDetailsService crowdUserDetailsService() throws IOException {
    CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
    cusd.setCrowdClient(crowdClient());
    cusd.setAuthorityPrefix("");
    return cusd;
  }

  @Bean
  public RemoteCrowdAuthenticationProvider crowdAuthenticationProvider() throws IOException {
    return new RemoteCrowdAuthenticationProvider(
        crowdClient(), httpAuthenticator(), crowdUserDetailsService());
  }

  @Bean
  public HttpSessionListener httpSessionListener() {

    return new ProvAppHttpSessionListener(
        authentication -> {
          String username = null;
          try {
            if (authentication.getPrincipal() instanceof CrowdUserDetails) {
              CrowdUserDetails userDetails = (CrowdUserDetails) authentication.getPrincipal();
              username = userDetails.getUsername();
            }
          } catch (Exception ex) {
            logger.debug("Extract username from authentication failed! [{}]", ex.getMessage());
          }
          return username;
        });
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/").allowedOrigins("http://localhost:4200");
      }
    };
  }

  @Bean
  public BasicAuthenticationEntryPoint basicAuthEntryPoint() {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(idManagerRealm);
    return entryPoint;
  }

  @Bean
  public String getCrowdServerUrl() {
    return crowdServerUrl;
  }
}
