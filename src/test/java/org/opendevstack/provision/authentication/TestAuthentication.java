package org.opendevstack.provision.authentication;

import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class TestAuthentication implements Authentication {

  public TestAuthentication() {}

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> auths = new ArrayList<>();
    auths.add(new TestAuthority());
    return auths;
  }

  @Override
  public Object getCredentials() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getDetails() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getPrincipal() {
    SOAPPrincipal principal = new SOAPPrincipal();
    principal.setName("clemens");

    CrowdUserDetails details =
        new CrowdUserDetails(principal, getAuthorities().toArray(new GrantedAuthority[] {}));
    return details;
  }

  @Override
  public boolean isAuthenticated() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    // TODO Auto-generated method stub

  }

  public class TestAuthority implements GrantedAuthority {

    @Override
    public String getAuthority() {
      return "testgroup";
    }
  }
}
