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

import javax.servlet.http.HttpSessionListener;
import org.opendevstack.provision.authentication.ProvAppHttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/** Configuration Class for the OAuth2 Spring Security Adapter */
@Configuration
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
@EnableWebSecurity
@EnableOAuth2Client
public class Oauth2SecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(Oauth2SecurityConfiguration.class);

  public static final String LOGIN_URI = "/login";

  @Autowired private RoleAwareOAuth2UserService roleAwareOAuth2UserService;

  @Autowired private Oauth2LogoutHandler oauth2LogoutHandler;

  @Autowired(required = false)
  private BasicAuthenticationEntryPoint basicAuthEntryPoint;

  @Autowired(required = false)
  private AuthenticationProvider basicAuthProvider;

  @Override
  public void configure(WebSecurity web) throws Exception {
    super.configure(web);
    web.ignoring()
        .antMatchers("/fragments/**", "/webjars/**", "/js/**", "/json/**", "/favicon.ico");
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) {

    if (basicAuthProvider != null) {
      LOG.info("Added authentication provider 'basicAuthProvider' to enable Basic Auth!");
      auth.authenticationProvider(basicAuthProvider);
    }
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    HttpSecurity sec =
        http.headers()
            .httpStrictTransportSecurity()
            .disable()
            .and()
            .cors()
            .disable()
            .csrf()
            .disable()
            .authorizeRequests()
            .antMatchers(
                "/",
                "/j_security_check/**",
                "/fragments/**",
                "/webjars/**",
                "/js/**",
                "/json/**",
                "/favicon.ico",
                LOGIN_URI)
            .permitAll()
            .anyRequest()
            .authenticated()
            .and();

    if (basicAuthEntryPoint != null) {
      LOG.info(
          "Added authentication entry point 'basicAuthenticationEntryPoint' to enable Basic Auth!");
      sec.httpBasic().authenticationEntryPoint(basicAuthEntryPoint);
    }

    sec.oauth2Login()
        // .loginPage(LOGIN_URI)
        .failureUrl("/login?error")
        .defaultSuccessUrl("/home")
        .userInfoEndpoint()
        .oidcUserService(roleAwareOAuth2UserService);

    http.logout()
        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
        .addLogoutHandler(oauth2LogoutHandler)
        .logoutSuccessUrl(LOGIN_URI)
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID")
        .permitAll();
  }

  @Bean
  public HttpSessionListener httpSessionListener() {
    return new ProvAppHttpSessionListener(ProvAppHttpSessionListener.createUsernameProvider());
  }
}
