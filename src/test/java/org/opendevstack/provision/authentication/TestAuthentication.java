package org.opendevstack.provision.authentication;

import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class TestAuthentication implements Authentication {

  private String username = "clemens";
  private String credentials;
  private GrantedAuthority[] authorities;
  private boolean authenticated;

  public TestAuthentication() {}

  public TestAuthentication(String username, String credentials, GrantedAuthority[] authorities) {
    this.username = username;
    this.credentials = credentials;
    this.authorities = authorities;
  }

  @Override
  public String getName() {
    return username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (authorities != null) {
      return List.of(authorities);
    } else {
      List<GrantedAuthority> auths = new ArrayList<>();
      auths.add(new TestAuthority());
      return auths;
    }
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {

    SOAPPrincipal principal = new SOAPPrincipal();
    principal.setName(username);

    if (authorities != null) {
      return new CrowdUserDetails(principal, authorities);
    } else {
      return new CrowdUserDetails(principal, getAuthorities().toArray(new GrantedAuthority[] {}));
    }
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    this.authenticated = isAuthenticated;
  }

  public class TestAuthority implements GrantedAuthority {

    @Override
    public String getAuthority() {
      return "testgroup";
    }
  }
}
