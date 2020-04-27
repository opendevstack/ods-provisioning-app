package org.opendevstack.provision.authentication.oauth2;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@RunWith(MockitoJUnitRunner.class)
public class RoleAwareOAuth2UserServiceTest {

  @Mock private DefaultOidcUser oidcUser;

  public static final String ROLE_1 = "Role1";
  private ObjectMapper mapper = new ObjectMapper();
  private String userRolesExpression = "/roles";

  private Token token;
  private JsonNode jwt;

  @Before
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

    Assert.assertEquals(email, RoleAwareOAuth2UserService.resolveUsername(oidcUser, true));
    Assert.assertEquals(username, RoleAwareOAuth2UserService.resolveUsername(oidcUser, false));

    try {
      RoleAwareOAuth2UserService.resolveUsername(null, false);
      fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("oidcUser"));
    }
  }

  @Test
  public void whenInvalidArgumentOrExpressionThrowError() {

    try {
      RoleAwareOAuth2UserService.extractRoles(null, userRolesExpression, false);
      fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("token"));
    }

    try {
      RoleAwareOAuth2UserService.extractRoles(jwt, null, false);
      fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("userRolesExpression"));
    }

    try {
      RoleAwareOAuth2UserService.extractRoles(jwt, "does-not-start-with-slash", false);
      fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("must start with '/'"));
    }
  }

  @Test
  public void whenValidUserRolesExpressionThenMappedRolesIsNotEmpty() {

    boolean convertToLowerCase = true;
    List<String> mappedRoles =
        RoleAwareOAuth2UserService.extractRoles(jwt, userRolesExpression, convertToLowerCase);

    Assert.assertEquals(convertToLowerCase, mappedRoles.contains(ROLE_1.toLowerCase()));
    Assert.assertEquals(!convertToLowerCase, mappedRoles.contains(ROLE_1));

    convertToLowerCase = false;
    mappedRoles =
        RoleAwareOAuth2UserService.extractRoles(jwt, userRolesExpression, convertToLowerCase);

    Assert.assertEquals(convertToLowerCase, mappedRoles.contains(ROLE_1.toLowerCase()));
    Assert.assertEquals(!convertToLowerCase, mappedRoles.contains(ROLE_1));
  }

  @Test
  public void whenNotValidUserRolesExpressionThenMappedRolesEmpty() {

    List<String> roles = RoleAwareOAuth2UserService.extractRoles(jwt, "/doesnotexists", false);
    Assert.assertNotNull(roles);
  }

  public class Token {

    @JsonSerialize private String id = "id";

    @JsonSerialize private List<String> roles = new ArrayList<>();

    public Token(List<String> list) {
      roles.addAll(list);
    }
  }
}
