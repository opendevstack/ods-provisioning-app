package org.opendevstack.provision.authentication.oauth2;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.Optional;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class Oauth2AuthenticationManager implements IODSAuthnzAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

  @Value("${provision.auth.provider.oauth2.user-info-uri}")
  private String userInfoUri;

  @Override
  public String getUserPassword() {
    throw new UnsupportedOperationException("not supported in oauth2 or basic auth authentication");
  }

  /** @see IODSAuthnzAdapter#getUserName() */
  public String getUserName() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (DefaultOidcUser.class.isInstance(principal)) {
      return ((DefaultOidcUser) principal).getEmail();
    } else if (CrowdUserDetails.class.isInstance(principal)) {
      return ((CrowdUserDetails) principal).getUsername();
    } else {
      throw new RuntimeException(
          String.format(
              "Unexpected error! Contact developers! Unsupported Principal object class '%s'! Supported Principal classes are String or DefaultOAuth2User",
              principal.getClass()));
    }
  }

  /** @see IODSAuthnzAdapter#getToken() */
  public String getToken() {
    throw new UnsupportedOperationException("not supported in oauth2 or basic auth authentication");
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
    throw new UnsupportedOperationException("not supported in oauth2 or basic auth authentication");
  }

  @Override
  public void setUserName(String userName) {
    throw new UnsupportedOperationException("not supported in oauth2 or basic auth authentication");
  }

  @Override
  public void invalidateIdentity() {
    LOG.debug("nothing to do here!");
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
