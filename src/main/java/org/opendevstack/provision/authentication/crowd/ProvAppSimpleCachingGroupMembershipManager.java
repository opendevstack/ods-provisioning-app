package org.opendevstack.provision.authentication.crowd;

import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.service.GroupManager;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.cache.BasicCache;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;
import org.opendevstack.provision.authentication.SimpleCachingGroupMembershipManager;

public class ProvAppSimpleCachingGroupMembershipManager
    extends SimpleCachingGroupMembershipManager {

  private final boolean lowercaseGroupNames;

  public ProvAppSimpleCachingGroupMembershipManager(
      SecurityServerClient securityServerClient,
      UserManager userManager,
      GroupManager groupManager,
      BasicCache cache,
      boolean lowercaseGroupNames) {
    super(securityServerClient, userManager, groupManager, cache);
    this.lowercaseGroupNames = lowercaseGroupNames;
  }

  @Override
  public List getMemberships(String user)
      throws RemoteException, InvalidAuthorizationTokenException, UserNotFoundException,
          InvalidAuthenticationException {
    List<String> memberships = super.getMemberships(user);
    if (lowercaseGroupNames) {
      List<String> membershipsAsLowercase =
          memberships.stream().map(group -> group.toLowerCase()).collect(Collectors.toList());
      return membershipsAsLowercase;
    } else {
      return memberships;
    }
  }
}
