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
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.util.Assert;

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

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${oauth2.user.roles.jsonpointerexpression}")
  private String userRolesExpression;

  @Value("${oauth2.user.use-email-claim-as-username:false}")
  private boolean useEmailClaimAsUserName;

  @Value("${oauth2.user.roles.convert-to-lower-case:true}")
  private boolean convertRolesToLowerCase;

  @Value("${oauth2.user.roles.keep-only-opendevstack-roles-from-jwt:true}")
  private boolean extractOnlyOpendevstackRoles;

  @Qualifier("opendevstackRoles")
  @Autowired
  private List<String> opendevstackRoles;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    // Delegate to the default implementation for loading a user
    OidcUser oidcUser = delegate.loadUser(userRequest);

    // Fetch the authority information from the protected resource using idToken
    Collection<GrantedAuthority> mappedAuthorities =
        extractAuthorities(userRequest, extractOnlyOpendevstackRoles);
    mappedAuthorities.addAll(oidcUser.getAuthorities());

    // Create a copy of oidcUser but use the mappedAuthorities instead
    DefaultOidcUser oidcUserWithAuthorities =
        new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    return oidcUserWithAuthorities;
  }

  private Collection<GrantedAuthority> extractAuthorities(
      OidcUserRequest userRequest, boolean keepOnlyOpendevstackRoles) {
    JsonNode token = objectMapper.convertValue(userRequest.getIdToken(), JsonNode.class);
    LOG.debug("Begin extractRoles at path '{}' from idToken jwt", userRolesExpression);

    try {
      List<String> roles = extractRoles(token, userRolesExpression, convertRolesToLowerCase);

      roles =
          keepOnlyOpendevstackRoles
              ? extractOnlyOpendevstackRoles(roles, opendevstackRoles)
              : roles;

      if (roles.isEmpty()) {
        LOG.warn(
            "Role extraction with expression '{}' was not successful. It returned an empty list!",
            userRolesExpression);
      }

      return AuthorityUtils.createAuthorityList(roles.toArray(new String[0]));
    } catch (IllegalArgumentException e) {
      LOG.warn("Cannot extract roles from id token:", e);
      return Collections.emptyList();
    }
  }

  public static List<String> extractRoles(
      JsonNode token, String userRolesExpression, boolean convertRolesToLowerCase) {
    Assert.notNull(token, "Parameter 'token' is null!");
    Assert.notNull(userRolesExpression, "Parameter 'userRolesExpression' is null!");

    return Collections.unmodifiableList(
        StreamSupport.stream(token.at(userRolesExpression).spliterator(), false)
            .map(JsonNode::asText)
            .map(roleName -> convertRolesToLowerCase ? roleName.toLowerCase() : roleName)
            .collect(Collectors.toList()));
  }

  public static String resolveUsername(OidcUser oidcUser, boolean useEmailClaimAsUserName) {
    Assert.notNull(oidcUser, "Parameter 'oidcUser' is null!");
    return useEmailClaimAsUserName ? oidcUser.getEmail() : oidcUser.getName();
  }

  public static List<String> extractOnlyOpendevstackRoles(
      List<String> roles, List<String> opendevstackRoles) {
    Assert.notNull(roles, "Parameter roles is null!");
    Assert.notNull(opendevstackRoles, "Parameter opendevstackRoles is null!");
    return roles.stream()
        .filter(role -> opendevstackRoles.contains(role))
        .collect(Collectors.toList());
  }
}
