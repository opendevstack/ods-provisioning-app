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

import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.model.authentication.AuthenticationContext;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;


import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import com.google.common.base.Preconditions;
import java.rmi.RemoteException;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.SessionAwarePasswordHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/** Custom Authentication manager to integrate the password storing for authentication */
@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "crowd")
public class CrowdAuthenticationManager implements AuthenticationManager, IODSAuthnzAdapter {

  private static final Logger logger = LoggerFactory.getLogger(CrowdAuthenticationManager.class);
  private CrowdClient crowdClient;

  @Autowired private SessionAwarePasswordHolder userPassword;

  /**
   * Constructor with secure SOAP restClient for crowd authentication
   *
   * @param crowdClient
   */
  public CrowdAuthenticationManager(CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

  /** @see IODSAuthnzAdapter#getUserPassword() */
  public String getUserPassword() {
    return userPassword.getPassword();
  }

  /** @see IODSAuthnzAdapter#getUserName() */
  public String getUserName() {
    return userPassword.getUsername();
  }

  /** @see IODSAuthnzAdapter#getToken() */
  public String getToken() {
    return userPassword.getToken();
  }

  /** @see IODSAuthnzAdapter#getUserEmail() () */
  public String getUserEmail() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return null;
    }

    if (!(auth.getPrincipal() instanceof CrowdUserDetails)) {
      return null;
    }

    CrowdUserDetails userDetails = (CrowdUserDetails) auth.getPrincipal();

    return userDetails.getEmail();
  }

  /** open for testing */
  public void setUserPassword(String userPassword) {
    this.userPassword.setPassword(userPassword);
  }

  /** open for testing */
  public void setUserName(String userName) {
    this.userPassword.setUsername(userName);
  }

  /**
   * specific authentication method implementation for crowd integration
   *
   * @param authenticationContext the auth context passed from spring
   * @return the user's token
   * @throws RemoteException
   * @throws InvalidAuthorizationTokenException
   * @throws InactiveAccountException
   * @throws ApplicationAccessDeniedException
   * @throws ExpiredCredentialException
   */
  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication);

/*
    if (authenticationContext.getApplication() == null) {
      //TODO

      authenticationContext.setApplication(
          this.getCrowdClient().getApplicationName());
    }

    String token = this.getCrowdClient().authenticateSSOUser(authenticationContext);
    userPassword.setToken(token);
    userPassword.setUsername(authenticationContext.getName());
    userPassword.setPassword(authenticationContext.getCredential().getCredential());*/
    return null;
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
  /*
  @Override
  public String authenticateWithoutValidatingPassword(
      UserAuthenticationContext authenticationContext)
      throws ApplicationAccessDeniedException, InvalidAuthenticationException,
          InvalidAuthorizationTokenException, InactiveAccountException, RemoteException {
    Preconditions.checkNotNull(authenticationContext);
    return this.getCrowdClient()
        .createPrincipalToken(
            authenticationContext.getName(), authenticationContext.getValidationFactors());
  }*/

  /**
   * simple authentication with username and password
   *
   * @param username users username
   * @param password users password
   * @return the users token
   * @throws RemoteException
   * @throws InvalidAuthorizationTokenException
   * @throws InvalidAuthenticationException
   * @throws InactiveAccountException
   * @throws ApplicationAccessDeniedException
   * @throws ExpiredCredentialException
   */
  //@Override
  public String authenticate(String username, String password)
          throws InvalidAuthenticationException,
          InactiveAccountException, ExpiredCredentialException, UserNotFoundException, OperationFailedException, ApplicationPermissionException {

    Preconditions.checkNotNull(username);
    Preconditions.checkNotNull(password);
    User user = this.getCrowdClient().authenticateUser(username, password);
    // TODO
    // userPassword.setToken();
    userPassword.setUsername(username);
    userPassword.setPassword(password);
    // TODO
    return "token";
  }

  /**
   * proof if user is authenticated
   *
   * @param token the users token
   * @param validationFactors and additional auth factors, eg. IP
   * @return true in case the token is valid
   * @throws RemoteException
   * @throws InvalidAuthorizationTokenException
   * @throws ApplicationAccessDeniedException
   * @throws InvalidAuthenticationException
   */
  /*
  @Override
  public boolean isAuthenticated(String token, ValidationFactor[] validationFactors)
      throws RemoteException, InvalidAuthorizationTokenException, ApplicationAccessDeniedException,
          InvalidAuthenticationException {
    Preconditions.checkNotNull(token);
    userPassword.setToken(token);
    this.getCrowdClient().validateSSOAuthentication(token, validationFactors.l);
    return true;
  }*/

  /**
   * Invalidate a session based on a user#s token
   *
   * @param token the users token
   * @throws RemoteException
   * @throws InvalidAuthorizationTokenException
   * @throws InvalidAuthenticationException
   */

  @Override
  public void invalidate(String token)
          throws InvalidAuthenticationException, OperationFailedException, ApplicationPermissionException {
    Preconditions.checkNotNull(token);
    this.getCrowdClient().invalidateSSOToken(token);
    userPassword.clear();
  }

  @Override
  public void invalidateIdentity() throws OperationFailedException, ApplicationPermissionException, InvalidAuthenticationException {
    invalidate(getToken());
  }

  /**
   * get the internal secure restClient
   *
   * @return the secure restClient for crowd connect
   */
  public CrowdClient getCrowdClient() {
    return this.crowdClient;
  }

  @Override
  public boolean existsGroupWithName(String groupName) {
    try {
      //TODO
      //crowdClient.findGroupByName(groupName);
      return true;
    } catch (Exception exception) {
      if (!(exception instanceof GroupNotFoundException)) {
        logger.error("GroupFind call failed with:", exception);
      }
      return false;
    }
  }

  @Override
  public boolean existPrincipalWithName(String userName) {
    try {
      // TODO
      //getCrowdClient().findPrincipalByName(userName);
      return true;
    } catch (Exception exception) {
      if (!(exception instanceof UsernameNotFoundException)) {
        logger.error("UserFind call failed with:", exception);
      }
      return false;
    }
  }

  @Override
  public String addGroup(String groupName) throws IdMgmtException {
    try {
      String name = "";
          // TODO
          //crowdClient.addGroup(new SOAPGroup(groupName, new String[] {})).getName();
      return name;
    } catch (Exception ex) {
      logger.error("Could not create group {}, error: {}", groupName, ex.getMessage(), ex);
      throw new IdMgmtException(ex);
    }
  }

  @Override
  public String getAdapterApiUri() {
    //return crowdClient.getSoapClientProperties().getBaseURL();
    //TODO
    return "";
  }

  /**
   * Set the secure restClient for injection in tests
   *
   * @param crowdClient
   */
  void setCrowdClient(CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

}
