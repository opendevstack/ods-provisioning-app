package org.opendevstack.provision.authentication.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.SessionAwarePasswordHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

@Component("provisioningAppAuthenticationManager")
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class Oauth2AuthenticationManager implements IODSAuthnzAdapter {

  @Autowired private SessionAwarePasswordHolder userPassword;

  @Override
  public String getUserPassword() {
    return userPassword.getPassword();
  }

  @Value("${spring.security.oauth2.client.provider.keycloak.user-info-uri}")
  private String userInfoUri;

  /** @see IODSAuthnzAdapter#getUserName() */
  public String getUserName() {
    return userPassword.getUsername();
  }

  /** @see IODSAuthnzAdapter#getToken() */
  public String getToken() {
    return userPassword.getToken();
  }

  /** @see IODSAuthnzAdapter#getAuthorities() */
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return new ArrayList<>();
    }

    DefaultOAuth2User userDetails = (DefaultOAuth2User) auth.getPrincipal();

    return userDetails.getAuthorities();
  }

  @Override
  public String getUserEmail() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(auth -> auth.getPrincipal() instanceof DefaultOidcUser)
        .map(auth -> (DefaultOidcUser) auth.getPrincipal())
        .map(StandardClaimAccessor::getEmail)
        .orElse(null);
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
  public void invalidateIdentity() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      setUserPassword(null);
    }
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
    // "Feature 'existsGroupWithName in OAuth 2 identity server' is not implemented"
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
    // "Feature 'existPrincipalWithName in OAuth 2 identity server' is not implemented"
    return false;
  }

  @Override
  public String addGroup(String groupName) throws IdMgmtException {
    throw new IdMgmtException(
        "Feature 'adding group in OAuth 2 identity server' is not implemented");
  }

  @Override
  public String getAdapterApiUri() {
    return userInfoUri;
  }
}
