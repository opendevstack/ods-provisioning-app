package org.opendevstack.provision.authentication.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Using Delegation-based strategy for reading OidcUser from {@link OidcUserService}, extracting
 * roles from access token and add them to a newly created copy of {@link OidcUser}
 *
 * @see <a
 *     href="https://docs.spring.io/spring-security/site/docs/5.2.x/reference/htmlsingle/#oauth2login-advanced-map-authorities-oauth2userservice">Delegation-based
 *     strategy with OAuth2UserService</a>
 * @author Stefan Lack
 */
@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class RoleAwareOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private static final Logger LOG = LoggerFactory.getLogger(RoleAwareOAuth2UserService.class);

  private final OidcUserService delegate = new OidcUserService();

  private final ObjectMapper objectMapper;

  private final String userRolesExpression;

  private final Oauth2AuthenticationManager authenticationManager;

  @Autowired
  public RoleAwareOAuth2UserService(
      ObjectMapper objectMapper,
      @Value("${oauth2.user.roles.jsonpointerexpression}") String userRolesExpression,
      Oauth2AuthenticationManager authenticationManager) {
    this.objectMapper = objectMapper;
    this.userRolesExpression = userRolesExpression;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    // Delegate to the default implementation for loading a user
    OidcUser oidcUser = delegate.loadUser(userRequest);

    // Fetch the authority information from the protected resource using idToken
    Collection<GrantedAuthority> mappedAuthorities = extractAuthorities(userRequest);
    mappedAuthorities.addAll(oidcUser.getAuthorities());

    authenticationManager.setUserName(oidcUser.getName());
    // authenticationManager.setEmail(oidcUser.getEmail());
    // Create a copy of oidcUser but use the mappedAuthorities instead
    DefaultOidcUser oidcUserWithAuthorities =
        new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    return oidcUserWithAuthorities;
  }

  private Collection<GrantedAuthority> extractAuthorities(OidcUserRequest userRequest) {
    JsonNode token = objectMapper.convertValue(userRequest.getIdToken(), JsonNode.class);
    LOG.debug("Begin extractRoles at path '{}' from idToken jwt = {}", userRolesExpression, token);

    try {
      List<String> roles =
          StreamSupport.stream(token.at(userRolesExpression).spliterator(), false)
              .map(JsonNode::asText)
              .collect(Collectors.toList());
      LOG.debug("End extractRoles: roles = {}", roles);

      return AuthorityUtils.createAuthorityList(roles.toArray(new String[0]));
    } catch (IllegalArgumentException e) {
      LOG.warn("Cannot extract roles from id token:", e);
      return Collections.emptyList();
    }
  }
}
