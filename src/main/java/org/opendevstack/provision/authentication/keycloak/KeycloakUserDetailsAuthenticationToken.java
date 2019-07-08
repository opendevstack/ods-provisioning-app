package org.opendevstack.provision.authentication.keycloak;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.Map;

public class KeycloakUserDetailsAuthenticationToken extends KeycloakAuthenticationToken {

  private UserDetails userDetails;

  public KeycloakUserDetailsAuthenticationToken(KeycloakAuthenticationToken token) {
    super(token.getAccount(), token.isInteractive(), token.getAuthorities());

    String username = this.resolveUsername(token);
    KeycloakUserDetails keycloakUserDetails =
        new KeycloakUserDetails(username, "N/A", token.getAuthorities());

    String email = readEmail(token);
    keycloakUserDetails.setEmail(email);
    this.userDetails = keycloakUserDetails;
  }

  /**
   * Reads the principals email adress from the token.
   *
   * @param token
   * @return
   */
  private String readEmail(KeycloakAuthenticationToken token) {
    KeycloakPrincipal principal = (KeycloakPrincipal) token.getPrincipal();
    return principal.getKeycloakSecurityContext().getToken().getEmail();
  }

  @Override
  public Object getPrincipal() {
    return userDetails;
  }

  /**
   * Returns the username from the given {@link KeycloakAuthenticationToken}. By default, this
   * method resolves the username from the token's {@link KeycloakPrincipal}'s name.
   */
  public static String resolveUsername(KeycloakAuthenticationToken token) {

    Assert.notNull(token, "KeycloakAuthenticationToken required");
    Assert.notNull(
        token.getAccount(), "KeycloakAuthenticationToken.getAccount() cannot be return null");
    OidcKeycloakAccount account = token.getAccount();
    Principal principal = account.getPrincipal();

    return principal.getName();
  }
}
