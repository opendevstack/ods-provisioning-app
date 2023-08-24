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

package org.opendevstack.provision.adapter;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import org.opendevstack.provision.adapter.exception.IdMgmtException;

/** Interface to wrap all (current) user based identity calls */
public interface IODSAuthnzAdapter {
  /**
   * Get the password from the logged in user
   *
   * @return password for the logged in user
   */
  public String getUserPassword();

  /**
   * Get the username of the currently logged in used
   *
   * @return the username
   */
  public String getUserName();

  /**
   * Get the user token, e.g. cookie of the currently logged in user
   *
   * @return the user's session token
   */
  public String getToken();

  /** Get the currently logged' in user's email */
  public String getUserEmail();

  void invalidate(String token)
          throws InvalidAuthenticationException, OperationFailedException, ApplicationPermissionException;

  /**
   * Invalidate the currently logged' in identity
   *
   * @throws Exception in case the identity cannot be invalidated
   */
  public void invalidateIdentity() throws Exception;

  void setUserPassword(String userPassword);

  boolean existsGroupWithName(String groupName);

  boolean existPrincipalWithName(String userName);

  String addGroup(String groupName) throws IdMgmtException;

  String getAdapterApiUri();

  void setUserName(String testUserName);
}
