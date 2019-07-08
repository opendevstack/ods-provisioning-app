package org.opendevstack.provision.authentication.keycloak;

import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.opendevstack.provision.authentication.SessionAwarePasswordHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "provision.auth.provider",
    havingValue = "keycloak",
    matchIfMissing = false)
public class KeycloakUserDetailsAuthenticationProvider extends KeycloakAuthenticationProvider {

  @Autowired private SessionAwarePasswordHolder userPassword;

  public KeycloakUserDetailsAuthenticationProvider() {

    // Map Authorities from type "KeycloakRole" to Type "SimpleGrantedAuthority",
    // since KeycloakRole has a differnt toString implementation
    SimpleAuthorityMapper authoritiesMapper = new SimpleAuthorityMapper();
    authoritiesMapper.setPrefix(""); // Do not add "ROLE_" - prefix.
    setGrantedAuthoritiesMapper(authoritiesMapper);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    KeycloakAuthenticationToken token =
        (KeycloakAuthenticationToken) super.authenticate(authentication);

    if (token == null) {
      return null;
    }

    KeycloakUserDetailsAuthenticationToken result =
        new KeycloakUserDetailsAuthenticationToken(token);
    userPassword.setUsername(result.getName());
    userPassword.setPassword("geheim");
    return result;
  }
}
