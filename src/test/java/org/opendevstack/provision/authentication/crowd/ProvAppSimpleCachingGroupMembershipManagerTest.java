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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.service.GroupManager;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.cache.BasicCache;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import java.rmi.RemoteException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

/** @author Sebastian Titakis */
@RunWith(SpringRunner.class)
public class ProvAppSimpleCachingGroupMembershipManagerTest {

  @MockBean private SecurityServerClient securityServerClient;

  @MockBean private UserManager userManager;

  @MockBean private GroupManager groupManager;

  @MockBean private BasicCache cache;

  @Test
  public void getMemberships()
      throws RemoteException, InvalidAuthorizationTokenException, UserNotFoundException,
          InvalidAuthenticationException {

    String groupInUppercase = "GROUP";

    when(cache.getAllMemberships(anyString())).thenReturn(List.of(groupInUppercase));

    // case memberships are not converted to lowercase
    ProvAppSimpleCachingGroupMembershipManager membershipManager =
        new ProvAppSimpleCachingGroupMembershipManager(
            securityServerClient, userManager, groupManager, cache, false);

    List<String> memberships = membershipManager.getMemberships("user");
    assertTrue(memberships.contains(groupInUppercase));

    // case memberships are converted to lowercase
    membershipManager =
        new ProvAppSimpleCachingGroupMembershipManager(
            securityServerClient, userManager, groupManager, cache, true);

    memberships = membershipManager.getMemberships("user");
    assertTrue(memberships.contains(groupInUppercase.toLowerCase()));
  }
}
