package org.opendevstack.provision.authentication.keycloak;

import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.google.common.base.Preconditions;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.SessionAwarePasswordHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

@Component("provisioningAppAuthenticationManager")
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "keycloak")
public class KeycloakAuthenticationManager implements IODSAuthnzAdapter {

  @Autowired private SessionAwarePasswordHolder userPassword;



  @Override
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

  /** @see IODSAuthnzAdapter#getAuthorities() */
  public Collection<GrantedAuthority> getAuthorities() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return new ArrayList<>();
    }

    User userDetails = (User) auth.getPrincipal();

    return userDetails.getAuthorities();
  }

  @Override
  public String getUserEmail() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return null;
    }

    if (!(auth.getPrincipal() instanceof KeycloakUserDetails)) {
      return null;
    }

    KeycloakUserDetails userDetails = (KeycloakUserDetails) auth.getPrincipal();

    return userDetails.getEmail();
  }

  @Override
  public void setUserPassword(String userPassword) {
    this.userPassword.setPassword(userPassword);
  }

  /** open for testing */
  public void setUserName(String userName) {
    this.userPassword.setUsername(userName);
  }



  @Override
  public void invalidateIdentity () throws Exception
  {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      setUserPassword(null);
      //new SecurityContextLogoutHandler().logout(request, response, auth);
    };
  }
  /**
   * returns whether the a role with specified name exists in keycloak. (keycloak role = provision
   * app group)
   *
   * @param groupName
   * @return
   */
  @Override
  public boolean existsGroupWithName(String groupName) {
    // TODO stefanlack implement existsGroupWithName
    return false;
  }

  /**
   * returns whether the a user/principal with specified name exists in keycloak.
   *
   * @param userName
   * @return
   */
  @Override
  public boolean existPrincipalWithName(String userName) {
    // TODO stefanlack implement existPrincipalWithName
    return false;
  }

  @Override
  public String addGroup(String groupName) throws IdMgmtException {
    throw new IdMgmtException("Feature 'adding group/role to keycloak' is not implemented");
  }

  @Override
  public String getAdapterApiUri() {
      // TODO stefanlack implement getAdapterApiUri for keycloak authentifaction.
    return "";
  }
}
