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
package org.opendevstack.provision.authentication;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.service.GroupManager;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.cache.BasicCache;
import com.atlassian.crowd.service.cache.CachingGroupMembershipManager;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;

/**
 * Simple extension of <code>CachingGroupMembershipManager</code> to fix
 * https://github.com/opendevstack/ods-provisioning-app/issues/106
 * 
 * @author utschig
 */
public class SimpleCachingGroupMembershipManager
        extends CachingGroupMembershipManager
{
    /**
     * security server client
     */
    final SecurityServerClient securityServerClient;
    /**
     * cache
     */
    final BasicCache basicCache;

    private static final Logger logger = LoggerFactory
            .getLogger(SimpleCachingGroupMembershipManager.class);

    public SimpleCachingGroupMembershipManager(
            SecurityServerClient securityServerClient,
            UserManager userManager, GroupManager groupManager,
            BasicCache basicCache)
    {
        super(securityServerClient, userManager, groupManager,
                basicCache);
        this.securityServerClient = securityServerClient;
        this.basicCache = basicCache;
    }

    @Override
    public List getMemberships(String user) throws RemoteException,
            InvalidAuthorizationTokenException,
            InvalidAuthenticationException, UserNotFoundException
    {
        List<String> groupsForUser = basicCache
                .getAllMemberships(user);
        if (groupsForUser == null || groupsForUser.isEmpty())
        {
            long startTime = System.currentTimeMillis();
            String[] groupMemberships = securityServerClient
                    .findGroupMemberships(user);

            if (groupMemberships == null)
            {
                return new ArrayList<>();
            }

            for (String group : groupMemberships)
            {
                basicCache.addGroupToUser(user, group);
                logger.debug("add group/user to cache ({} / {})",
                        user, group);
            }
            logger.debug("add to cache ({}) took: {} ms", user,
                    (System.currentTimeMillis() - startTime));

            return new ArrayList<>(Arrays.asList(groupMemberships));
        } else
        {
            long startTime = System.currentTimeMillis();
            for (String group : groupsForUser)
            {
                logger.debug("retrieve from cache ({} / {})", user,
                        group);
            }
            logger.debug("retrieve from cache ({}) took: {} ms", user,
                    (System.currentTimeMillis() - startTime));
            return groupsForUser;
        }
    }
}
