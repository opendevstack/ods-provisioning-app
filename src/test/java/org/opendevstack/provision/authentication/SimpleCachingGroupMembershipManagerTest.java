package org.opendevstack.provision.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsServiceImpl;
import com.atlassian.crowd.service.GroupManager;
import com.atlassian.crowd.service.UserManager;
import com.atlassian.crowd.service.cache.BasicCache;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
/**
 * Testclass for the simple caching manager
 * 
 * @author utschig
 *
 */
public class SimpleCachingGroupMembershipManagerTest {

  @Mock
  private SecurityServerClient securityServerClient;

  @Mock
  private UserManager usermanager;

  @Mock
  private GroupManager groupmanager;

  @Autowired
  private BasicCache basicCache;

  @Test
  public void testSingleUserLookupWithGroupsAndCaching() throws Exception {
    SOAPPrincipal user1 = new SOAPPrincipal();
    user1.setName("someuser1");

    // known user with 2 roles
    Mockito.when(usermanager.getUserFromToken(user1.getName())).thenReturn(user1);

    Mockito.when(securityServerClient.findGroupMemberships(user1.getName()))
        .thenReturn(new String[] {"role2", "role1"});

    CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
    cusd.setUserManager(usermanager);
    cusd.setGroupMembershipManager(new SimpleCachingGroupMembershipManager(securityServerClient,
        usermanager, groupmanager, basicCache));
    cusd.setAuthorityPrefix("");

    CrowdUserDetails details = cusd.loadUserByToken(user1.getName());
    assertNotNull(details);

    // 2 authorities come back
    assertSame(2, details.getAuthorities().size());

    Mockito.verify(securityServerClient).findGroupMemberships(user1.getName());
    details = cusd.loadUserByToken(user1.getName());
    assertNotNull(details);

    // 2 authorities come back - but no new external call
    assertSame(2, details.getAuthorities().size());
    Mockito.verify(securityServerClient).findGroupMemberships(user1.getName());
  }

  @Test
  public void testUserLookupNoGroups() throws Exception {
    SOAPPrincipal user1 = new SOAPPrincipal();
    user1.setName("someuser10");

    // known user with NULL roles .. just to ensure that a null
    // does not blow up
    Mockito.when(usermanager.getUserFromToken(user1.getName())).thenReturn(user1);

    // Mockito.when(securityServerClient.findGroupMemberships(user1.getName())).
    // thenReturn(new String[] {});

    CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
    cusd.setUserManager(usermanager);
    cusd.setGroupMembershipManager(new SimpleCachingGroupMembershipManager(securityServerClient,
        usermanager, groupmanager, basicCache));
    cusd.setAuthorityPrefix("");

    CrowdUserDetails details = cusd.loadUserByToken(user1.getName());
    assertNotNull(details);

    assertSame(0, details.getAuthorities().size());
  }

  @Test
  public void testMultipleUserLookupWithGroups() throws Exception {
    SOAPPrincipal user1 = new SOAPPrincipal();
    user1.setName("someuser");

    // known user with 2 roles
    Mockito.when(usermanager.getUserFromToken(user1.getName())).thenReturn(user1);

    Mockito.when(securityServerClient.findGroupMemberships(user1.getName()))
        .thenReturn(new String[] {"role2", "role1"});

    SOAPPrincipal user2 = new SOAPPrincipal();
    user2.setName("someuser2");

    // known user with 2 roles
    Mockito.when(usermanager.getUserFromToken(user2.getName())).thenReturn(user2);

    Mockito.when(securityServerClient.findGroupMemberships(user2.getName()))
        .thenReturn(new String[] {"role3", "role4"});

    CrowdUserDetailsServiceImpl cusd = new CrowdUserDetailsServiceImpl();
    cusd.setUserManager(usermanager);
    cusd.setGroupMembershipManager(new SimpleCachingGroupMembershipManager(securityServerClient,
        usermanager, groupmanager, basicCache));
    cusd.setAuthorityPrefix("");

    // load user 1 - 2 roles come back ..
    CrowdUserDetails details = cusd.loadUserByToken(user1.getName());
    assertSame(2, details.getAuthorities().size());

    assertEquals("[role2, role1]", details.getAuthorities().toString());

    // load user 2 ... two other roles come back
    details = cusd.loadUserByToken(user2.getName());
    assertNotNull(details);
    assertEquals("[role3, role4]", details.getAuthorities().toString());
  }

}
