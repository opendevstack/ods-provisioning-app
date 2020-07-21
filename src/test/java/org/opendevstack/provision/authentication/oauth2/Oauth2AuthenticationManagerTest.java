package org.opendevstack.provision.authentication.oauth2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DirtiesContext
public class Oauth2AuthenticationManagerTest {

  @MockBean private DefaultOidcUser defaultOidcUser;

  @MockBean private CrowdUserDetails crowdUserDetails;

  @Test
  public void givenUserLoggedInWithOAuth2_whenOAuth2ManagerResolvesGetUserName_thenReturnEmail() {

    SecurityContext contextHolder = TestSecurityContextHolder.getContext();

    String email = "firstname.lastname@example.com";
    when(defaultOidcUser.getEmail()).thenReturn(email);

    Authentication authentication = new CustomAuthentication(defaultOidcUser);
    contextHolder.setAuthentication(authentication);

    Oauth2AuthenticationManager manager = new Oauth2AuthenticationManager();

    assertEquals(email, manager.getUserName());
  }

  @Test
  public void
      givenUserLoggedInWithBasicAuth_whenOAuth2ManagerResolvesGetUserName_thenBasicAuthUsername() {

    SecurityContext contextHolder = TestSecurityContextHolder.getContext();

    String username = "username";

    when(crowdUserDetails.getUsername()).thenReturn(username);

    Authentication authentication = new CustomAuthentication(crowdUserDetails);

    contextHolder.setAuthentication(authentication);

    Oauth2AuthenticationManager manager = new Oauth2AuthenticationManager();

    assertEquals(username, manager.getUserName());
  }

  @Test
  public void
      givenNotSupportedAuthenticationWasConfigured_whenOAuth2ManagerResolvesGetUserName_thenException() {

    SecurityContext contextHolder = TestSecurityContextHolder.getContext();

    Authentication authentication = new CustomAuthentication("string_as_principal");

    contextHolder.setAuthentication(authentication);

    Oauth2AuthenticationManager manager = new Oauth2AuthenticationManager();

    try {
      manager.getUserName();
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Unsupported Principal object"));
    }
  }

  private class CustomAuthentication implements Authentication {

    private Object principal;

    private CustomAuthentication(Object principal) {
      this.principal = principal;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return principal;
    }

    @Override
    public boolean isAuthenticated() {
      return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

    @Override
    public String getName() {
      return null;
    }
  }
}
