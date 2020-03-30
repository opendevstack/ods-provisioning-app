package org.opendevstack.provision.authentication.oauth2;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@RunWith(MockitoJUnitRunner.class)
public class RoleAwareOAuth2UserServiceTest {

  @Mock private DefaultOidcUser oidcUser;

  @Test
  public void whenUseEmailClaimAsUsernameThenUseEmail() {

    String username = "username";
    String email = "useremail@notexists.com";

    when(oidcUser.getEmail()).thenReturn(email);
    when(oidcUser.getName()).thenReturn(username);

    Assert.assertEquals(email, RoleAwareOAuth2UserService.resolveUsername(oidcUser, true));
    Assert.assertEquals(username, RoleAwareOAuth2UserService.resolveUsername(oidcUser, false));

    try {
      RoleAwareOAuth2UserService.resolveUsername(null, false);
      fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("oidcUser"));
    }
  }
}
