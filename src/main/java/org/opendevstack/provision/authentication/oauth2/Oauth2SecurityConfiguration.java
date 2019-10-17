package org.opendevstack.provision.authentication.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuration Class for the Keycloak Spring Security Adapter
 *
 * @author Stefan Lack
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
@EnableWebSecurity
@EnableOAuth2Client
public class Oauth2SecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final String LOGIN_URI = "/login";

  @Autowired private RoleAwareOAuth2UserService roleAwareOAuth2UserService;
  @Autowired private Oauth2LogoutHandler oauth2LogoutHandler;

  @Override
  public void configure(WebSecurity web) throws Exception {
    super.configure(web);
    web.ignoring()
        .antMatchers("/fragments/**", "/webjars/**", "/js/**", "/json/**", "/favicon.ico");
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
}
