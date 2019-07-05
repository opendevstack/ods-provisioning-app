/*
 * Copyright 2018 the original author or authors.
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

package org.opendevstack.provision.authentication;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.atlassian.crowd.exception.ApplicationAccessDeniedException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;
import com.atlassian.crowd.model.authentication.ValidationFactor;
import com.atlassian.crowd.service.AuthenticationManager;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import com.google.common.base.Preconditions;

/**
 * Custom Authentication manager to integrate the password storing for rundeck
 * authentication
 *
 * @author Torsten Jaeschke
 */
@Component
public class CustomAuthenticationManager
        implements AuthenticationManager, IODSAuthnzAdapter
{

    private SecurityServerClient securityServerClient;

    @Autowired
    private SessionAwarePasswordHolder userPassword;

    /**
     * get the password from the logged in user
     *
     * @return password for the logged in user
     */
    public String getUserPassword()
    {
        return userPassword.getPassword();
    }
    
    public String getUserName ()
    {
        return userPassword.getUsername();
    }
    
    public String getToken ()
    {
        return userPassword.getToken();
    }

    public Collection<GrantedAuthority> getAuthorities () 
    {
        Authentication auth = SecurityContextHolder 
                .getContext().getAuthentication();
        
        if (auth == null) 
        {
            return new ArrayList<>();
        }
        
        CrowdUserDetails userDetails = (CrowdUserDetails)auth.getPrincipal();
        
        return userDetails.getAuthorities();
    }
    
    public String getUserEmail () 
    {
        Authentication auth = SecurityContextHolder 
                .getContext().getAuthentication();
        
        if (auth == null) 
        {
            return null;
        }
        
        if (!(auth.getPrincipal() instanceof CrowdUserDetails)) {
            return null;
        }
        
        CrowdUserDetails userDetails = (CrowdUserDetails)auth.getPrincipal();
        
        return userDetails.getEmail();
    }
    
    /**
     * testing
     */
    void setUserPassword(String userPassword)
    {
        this.userPassword.setPassword(userPassword);
    }

    public void setUserName(String userName)
    {
        this.userPassword.setUsername(userName);
    }
        
    /**
     * Constructor with secure SOAP client for crowd authentication
     * 
     * @param securityServerClient
     */
    public CustomAuthenticationManager(
            SecurityServerClient securityServerClient)
    {
        this.securityServerClient = securityServerClient;
    }

    /**
     * specific authentication method for crowd
     *
     * @param authenticationContext
     * @return
     * @throws RemoteException
     * @throws InvalidAuthorizationTokenException
     * @throws InvalidAuthenticationException
     * @throws InactiveAccountException
     * @throws ApplicationAccessDeniedException
     * @throws ExpiredCredentialException
     */
    public String authenticate(
            UserAuthenticationContext authenticationContext)
            throws RemoteException,
            InvalidAuthorizationTokenException,
            InvalidAuthenticationException, InactiveAccountException,
            ApplicationAccessDeniedException,
            ExpiredCredentialException
    {
        Preconditions.checkNotNull(authenticationContext);
        
        if (authenticationContext.getApplication() == null)
        {
            authenticationContext.setApplication(this
                    .getSecurityServerClient()
                    .getSoapClientProperties().getApplicationName());
        }

        String token = this.getSecurityServerClient()
                .authenticatePrincipal(authenticationContext);
        userPassword.setToken(token);
        userPassword.setUsername(authenticationContext.getName());
        userPassword.setPassword(authenticationContext.getCredential()
                .getCredential());
        return token;
    }

    /**
     * authenticate via crowd token
     *
     * @param authenticationContext
     * @return
     * @throws ApplicationAccessDeniedException
     * @throws InvalidAuthenticationException
     * @throws InvalidAuthorizationTokenException
     * @throws InactiveAccountException
     * @throws RemoteException
     */
    public String authenticateWithoutValidatingPassword(
            UserAuthenticationContext authenticationContext)
            throws ApplicationAccessDeniedException,
            InvalidAuthenticationException,
            InvalidAuthorizationTokenException,
            InactiveAccountException, RemoteException
    {
        Preconditions.checkNotNull(authenticationContext);
        return this.getSecurityServerClient().createPrincipalToken(
                authenticationContext.getName(),
                authenticationContext.getValidationFactors());
    }

    /**
     * simple authentication with username and password
     *
     * @param username
     * @param password
     * @return
     * @throws RemoteException
     * @throws InvalidAuthorizationTokenException
     * @throws InvalidAuthenticationException
     * @throws InactiveAccountException
     * @throws ApplicationAccessDeniedException
     * @throws ExpiredCredentialException
     */
    public String authenticate(String username, String password)
            throws RemoteException,
            InvalidAuthorizationTokenException,
            InvalidAuthenticationException, InactiveAccountException,
            ApplicationAccessDeniedException,
            ExpiredCredentialException
    {

        Preconditions.checkNotNull(username);
        Preconditions.checkNotNull(password);
        String token = this.getSecurityServerClient()
                .authenticatePrincipalSimple(username, password);
        userPassword.setToken(token);
        userPassword.setUsername(username);
        userPassword.setPassword(password);
        return token;
    }

    /**
     * proof if user is authenticated
     *
     * @param token
     * @param validationFactors
     * @return
     * @throws RemoteException
     * @throws InvalidAuthorizationTokenException
     * @throws ApplicationAccessDeniedException
     * @throws InvalidAuthenticationException
     */
    public boolean isAuthenticated(String token,
            ValidationFactor[] validationFactors)
            throws RemoteException,
            InvalidAuthorizationTokenException,
            ApplicationAccessDeniedException,
            InvalidAuthenticationException
    {
        Preconditions.checkNotNull(token);
        userPassword.setToken(token);
        return this.getSecurityServerClient().isValidToken(token,
                validationFactors);
    }

    /**
     * invalidate session
     *
     * @param token
     * @throws RemoteException
     * @throws InvalidAuthorizationTokenException
     * @throws InvalidAuthenticationException
     */
    public void invalidate(String token) throws RemoteException,
            InvalidAuthorizationTokenException,
            InvalidAuthenticationException
    {
        Preconditions.checkNotNull(token);
        this.getSecurityServerClient().invalidateToken(token);
        userPassword.clear();
    }

    @Override
    public void invalidateIdentity () throws Exception
    {
        invalidate(getToken());
    }
    
    /**
     * get the internal secure client
     *
     * @return the secure client for crowd connect
     */
    public SecurityServerClient getSecurityServerClient()
    {
        return this.securityServerClient;
    }

    /**
     * Set the secure client for injection in tests
     *
     * @param securityServerClient
     */
    public void setSecurityServerClient(
            SecurityServerClient securityServerClient)
    {
        this.securityServerClient = securityServerClient;
    }
}
