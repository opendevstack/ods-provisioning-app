package org.opendevstack.provision.authentication.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RoleAwareOAuth2UserServiceTest {

  @Mock private DefaultOidcUser oidcUser;

  public static final String ROLE_1 = "Role1";
  private ObjectMapper mapper = new ObjectMapper();
  private String userRolesExpression = "/roles";

  private Token token;
  private JsonNode jwt;

  @BeforeEach
  public void setup() {

    List<String> roles = new ArrayList();
    roles.add(ROLE_1);
    token = new Token(roles);
    jwt = mapper.valueToTree(token);
  }

  @Test
  public void whenUseEmailClaimAsUsernameThenUseEmail() {

    String username = "username";
    String email = "useremail@notexists.com";

    when(oidcUser.getEmail()).thenReturn(email);
    when(oidcUser.getName()).thenReturn(username);

    assertEquals(email, RoleAwareOAuth2UserService.resolveUsername(oidcUser, true));
    assertEquals(username, RoleAwareOAuth2UserService.resolveUsername(oidcUser, false));

    try {
      RoleAwareOAuth2UserService.resolveUsername(null, false);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("oidcUser"));
    }
  }

  @Test
  public void whenInvalidArgumentOrExpressionThrowError() {

    try {
      RoleAwareOAuth2UserService.extractRoles(null, userRolesExpression, false);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("token"));
    }

    try {
      RoleAwareOAuth2UserService.extractRoles(jwt, null, false);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("userRolesExpression"));
    }

    try {
      RoleAwareOAuth2UserService.extractRoles(jwt, "does-not-start-with-slash", false);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("must start with '/'"));
    }
  }

  @Test
  public void whenValidUserRolesExpressionThenMappedRolesIsNotEmpty() {

    boolean convertToLowerCase = true;
    List<String> mappedRoles =
        RoleAwareOAuth2UserService.extractRoles(jwt, userRolesExpression, convertToLowerCase);

    assertEquals(convertToLowerCase, mappedRoles.contains(ROLE_1.toLowerCase()));
    assertEquals(!convertToLowerCase, mappedRoles.contains(ROLE_1));

    convertToLowerCase = false;
    mappedRoles =
        RoleAwareOAuth2UserService.extractRoles(jwt, userRolesExpression, convertToLowerCase);

    assertEquals(convertToLowerCase, mappedRoles.contains(ROLE_1.toLowerCase()));
    assertEquals(!convertToLowerCase, mappedRoles.contains(ROLE_1));
  }

  @Test
  public void whenNotValidUserRolesExpressionThenMappedRolesEmpty() {

    List<String> roles = RoleAwareOAuth2UserService.extractRoles(jwt, "/doesnotexists", false);
    assertNotNull(roles);
  }

  @Test
  public void extractOnlyOpendevstackRoles() {

    String roleA = "A";
    String roleB = "B";
    String roleC = "C";

    List<String> allRoles = List.of(roleA, roleB, roleC);

    List<String> extracted =
        RoleAwareOAuth2UserService.extractOnlyOpendevstackRoles(allRoles, List.of(roleB));

    assertEquals(1, extracted.size());
    assertTrue(extracted.contains(roleB));

    try {
      RoleAwareOAuth2UserService.extractOnlyOpendevstackRoles(null, List.of(roleB));
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("roles"));
    }

    try {
      RoleAwareOAuth2UserService.extractOnlyOpendevstackRoles(allRoles, null);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("opendevstackRoles"));
    }
  }

  public class Token {

    @JsonSerialize private String id = "id";

    @JsonSerialize private List<String> roles = new ArrayList<>();

    public Token(List<String> list) {
      roles.addAll(list);
    }
  }
}
