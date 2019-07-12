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

package org.opendevstack.provision.authentication.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.authentication.SimpleCachingGroupMembershipManager;
import org.opendevstack.provision.authentication.filter.SSOAuthProcessingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.atlassian.crowd.integration.http.HttpAuthenticator;
import com.atlassian.crowd.integration.http.HttpAuthenticatorImpl;
import com.atlassian.crowd.integration.springsecurity.CrowdLogoutHandler;
import com.atlassian.crowd.integration.springsecurity.RemoteCrowdAuthenticationProvider;
import com.atlassian.crowd.integration.springsecurity.UsernameStoringAuthenticationFailureHandler;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsServiceImpl;
import com.atlassian.crowd.service.AuthenticationManager;
import com.atlassian.crowd.service.GroupManager;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.cache.BasicCache;
import com.atlassian.crowd.service.cache.CacheImpl;
import com.atlassian.crowd.service.cache.CachingGroupManager;
import com.atlassian.crowd.service.cache.CachingUserManager;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import com.atlassian.crowd.service.soap.client.SecurityServerClientImpl;
import com.atlassian.crowd.service.soap.client.SoapClientPropertiesImpl;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import net.sf.ehcache.CacheManager;

/**
 *
 * Class for setting the security configuration and security related
 * configurations
 *
 * @author Brokmeier, Pascal
 */
@Configuration
@EnableWebSecurity
@EnableCaching
@EnableEncryptableProperties
@ConditionalOnProperty(
        name = "provision.auth.provider",
        havingValue = "crowd",
        matchIfMissing = false)
public class SecurityConfiguration
        extends WebSecurityConfigurerAdapter
{

    /**
     * Configure the security for the spring application
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http.authenticationProvider(crowdAuthenticationProvider())
                .headers().httpStrictTransportSecurity().disable()
                .and().cors().disable().csrf().disable()
                .addFilter(crowdSSOAuthenticationProcessingFilter())
                .authorizeRequests()
                .antMatchers("/", "/fragments/**", "/webjars/**",
                        "/js/**", "/json/**", "/favicon.ico",
                        "/login")
                .permitAll().anyRequest().authenticated().and()
                .formLogin().loginPage("/login").permitAll()
                .defaultSuccessUrl("/home").and().logout()
                .addLogoutHandler(crowdLogoutHandler()).permitAll();
    }

    @Value("${crowd.application.name}")
    String crowdApplicationName;

    @Value("${crowd.application.password}")
    String crowdApplicationPassword;

    @Value("${crowd.server.url}")
    String crowdServerUrl;

    @Value("${crowd.cookie.domain}")
    String cookieDomain;

    /**
     * Get the properties used for crowd authentication
     *
     * @return
     * @throws IOException
     */
    public Properties getProps() throws IOException
    {

        Properties prop = new Properties();
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("crowd.properties"))
        {
            prop.load(in);
        }
        prop.setProperty("application.name", crowdApplicationName);
        prop.setProperty("application.password",
                crowdApplicationPassword);
        prop.setProperty("crowd.server.url", crowdServerUrl);
        prop.setProperty("cookie.domain", cookieDomain);
        return prop;
    }

    /**
     * Define a logout handler to perform a clean crowd logout
     *
     * @return
     * @throws IOException
     */
    @Bean
    public CrowdLogoutHandler crowdLogoutHandler() throws IOException
    {
        CrowdLogoutHandler clh = new CrowdLogoutHandler();
        clh.setHttpAuthenticator(httpAuthenticator());
        return clh;
    }

    /**
     * Set a filter intercepting the login form to process a crowd authentication
     *
     * @return
     * @throws Exception
     */
    @Bean
    public SSOAuthProcessingFilter crowdSSOAuthenticationProcessingFilter()
            throws Exception
    {
        SSOAuthProcessingFilter filter = new SSOAuthProcessingFilter();
        filter.setHttpAuthenticator(httpAuthenticator());
        filter.setAuthenticationManager(authenticationManager());
        filter.setFilterProcessesUrl("/j_security_check");
        filter.setAuthenticationSuccessHandler(
                authenticationSuccessHandler());
        filter.setAuthenticationFailureHandler(
                authenticationFailureHandler());
        filter.setUsernameParameter("username");
        filter.setPasswordParameter("password");
        return filter;
    }

    /**
     * Define a success handler to proceed after a crowd authentication, if it has
     * been successful
     *
     * @return
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler()
    {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/home");
        successHandler.setUseReferer(true);
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        return successHandler;
    }

    /**
     * define an failure handler in case of an authentication failure
     *
     * @return
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler()
    {
        UsernameStoringAuthenticationFailureHandler failureHandler = new UsernameStoringAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl("/login?error=true");
        failureHandler.setUseForward(true);
        return failureHandler;
    }

    /**
     * Define a bean for the soap client used to authenticate against crowd
     *
     * @return
     * @throws IOException
     */
    @Bean
    @ConditionalOnProperty(
            name = "provision.auth.provider",
            havingValue = "crowd",
            matchIfMissing = true)
    public SecurityServerClient securityServerClient()
            throws IOException
    {
        return new SecurityServerClientImpl(SoapClientPropertiesImpl
                .newInstanceFromProperties(getProps()));
    }

    /**
     * Define a basic cache for userdata caching
     * 
     * @return
     */
    @Bean
    public BasicCache getCache()
    {
        return new CacheImpl(getCacheManager());
    }

    /**
     * Define a cache manager for eh-cache
     * 
     * @return
     */
    @Bean
    public CacheManager getCacheManager()
    {
        return getEhCacheFactory().getObject();
    }

    @Bean
    public EhCacheManagerFactoryBean getEhCacheFactory()
    {
        EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
        factoryBean.setConfigLocation(
                new ClassPathResource("crowd-ehcache.xml"));
        return factoryBean;
    }

    /**
     * define a custom authentication manager, which is able to hold the password
     * for rundeck
     *
     * @return
     * @throws IOException
     */
    @Bean(name = "provisioningAppAuthenticationManager")
    public AuthenticationManager crowdAuthenticationManager()
            throws IOException
    {
        return new CustomAuthenticationManager(
                securityServerClient());
    }

    /**
     * Define the authenticator for the secure client
     *
     * @return
     * @throws IOException
     */
    @Bean
    public HttpAuthenticator httpAuthenticator() throws IOException
    {
        return new HttpAuthenticatorImpl(
                crowdAuthenticationManager());
    }

    /**
     * Define a custom user manager for handling crowd users
     * 
     * @return
     * @throws IOException
     */
    @Bean
    public UserManager userManager() throws IOException
    {
        return new CachingUserManager(securityServerClient(),
                getCache());
    }

    /**
     * Define a manager for handling crowd groups
     *
     * @return
     * @throws IOException
     */
    @Bean
    public GroupManager groupManager() throws IOException
    {
        return new CachingGroupManager(securityServerClient(),
                getCache());
    }

    /**
     * Configure the manager uses for the authentication provider
     *
     * @param auth
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception
    {
        auth.authenticationProvider(crowdAuthenticationProvider());
    }

    /**
     * Define crowd user details for usage in security context
     *
     * @return
     * @throws IOException
     */
    @Bean
    public CrowdUserDetailsService crowdUserDetailsService()
            throws IOException
    {
        CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
        cusd.setUserManager(userManager());
        cusd.setGroupMembershipManager(
                new SimpleCachingGroupMembershipManager(
                        securityServerClient(), userManager(),
                        groupManager(), getCache()));
        cusd.setAuthorityPrefix("");
        return cusd;
    }

    /**
     * Define the crowd authentication provider
     *
     * @return
     * @throws IOException
     */
    @Bean
    RemoteCrowdAuthenticationProvider crowdAuthenticationProvider()
            throws IOException
    {
        return new RemoteCrowdAuthenticationProvider(
                crowdAuthenticationManager(), httpAuthenticator(),
                crowdUserDetailsService());
    }
}
