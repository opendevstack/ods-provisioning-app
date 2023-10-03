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
import com.atlassian.crowd.model.group.ImmutableGroup;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Custom Authentication manager to integrate the password storing for authentication */
@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "crowd")
public class CrowdAuthenticationAdapter implements IODSAuthnzAdapter {
  private static final Logger logger = LoggerFactory.getLogger(CrowdAuthenticationAdapter.class);
  private CrowdClient crowdClient;

  @Autowired private SessionAwarePasswordHolder userPassword;

  /**
   * Constructor with secure SOAP restClient for crowd authentication
   *
   * @param crowdClient
   */
  public CrowdAuthenticationAdapter(CrowdClient crowdClient) {
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
   * Invalidate a session based on a user#s token
   *
   * @param token the users token
   * @throws RemoteException
   * @throws InvalidAuthorizationTokenException
   * @throws InvalidAuthenticationException
   */
  @Override
  public void invalidate(String token)
      throws InvalidAuthenticationException, OperationFailedException,
          ApplicationPermissionException {
    Preconditions.checkNotNull(token);
    this.getCrowdClient().invalidateSSOToken(token);
    userPassword.clear();
  }

  @Override
  public void invalidateIdentity()
      throws OperationFailedException, ApplicationPermissionException,
          InvalidAuthenticationException {
    invalidate(getToken());
  }

  @Override
  public boolean existsGroupWithName(String groupName) {
    try {
      crowdClient.getGroup(groupName);
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
      crowdClient.getUser(userName);
      return true;
    } catch (Exception exception) {
      if (!(exception instanceof UserNotFoundException)) {
        logger.error("UserFind call failed with:", exception);
      }
      return false;
    }
  }

  @Override
  public String addGroup(String groupName) throws IdMgmtException {
    try {
      crowdClient.addGroup(ImmutableGroup.builder(groupName).build());
      return groupName;
    } catch (Exception ex) {
      logger.error("Could not create group {}, error: {}", groupName, ex.getMessage(), ex);
      throw new IdMgmtException(ex);
    }
  }

  @Override
  public String getAdapterApiUri() {
    throw new UnsupportedOperationException("not supported in oauth2 or basic auth authentication");
  }

  /**
   * get the internal secure restClient
   *
   * @return the secure restClient for crowd connect
   */
  public CrowdClient getCrowdClient() {
    return this.crowdClient;
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
