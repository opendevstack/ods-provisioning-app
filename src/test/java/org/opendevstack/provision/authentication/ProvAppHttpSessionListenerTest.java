package org.opendevstack.provision.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ProvAppHttpSessionListenerTest {

  @MockBean private HttpSessionEvent sessionEvent;

  @MockBean private HttpSession httpSession;

  @MockBean private Authentication authentication;

  @Test
  public void givenSessionDestroyed_whenUsernameProviderFindName_thenOk()
      throws InterruptedException {

    SecurityContext context = spy(TestSecurityContextHolder.getContext());

    when(sessionEvent.getSession()).thenReturn(httpSession);
    when(httpSession.getId()).thenReturn(UUID.randomUUID().toString());
    when(httpSession.getCreationTime()).thenReturn(System.currentTimeMillis());
    when(httpSession.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
    when(httpSession.getMaxInactiveInterval()).thenReturn(100);
    when(httpSession.getAttribute(ProvAppHttpSessionListener.SPRING_SECURITY_CONTEXT))
        .thenReturn(context);
    when(context.getAuthentication()).thenReturn(authentication);

    CountDownLatch latch = new CountDownLatch(1);

    ProvAppHttpSessionListener provAppHttpSessionListener =
        new ProvAppHttpSessionListener(
            auth -> {
              assertEquals(authentication, auth);
              latch.countDown();
              return "username";
            });

    provAppHttpSessionListener.sessionDestroyed(sessionEvent);

    boolean await = latch.await(100, TimeUnit.MILLISECONDS);
    assertTrue(await);
    assertEquals(0, latch.getCount());
  }

  @Test
  public void givenSessionDestroyed_whenNPE_ApplicationDoesNotBreak() {

    when(sessionEvent.getSession()).thenThrow(new NullPointerException());
    ProvAppHttpSessionListener provAppHttpSessionListener =
        new ProvAppHttpSessionListener(
            auth -> {
              return null;
            });

    provAppHttpSessionListener.sessionDestroyed(sessionEvent);
  }

  @Test
  public void createUsernameProvider() {

    String username = "username@example.com";

    Function<Authentication, String> usernameProvider =
        ProvAppHttpSessionListener.createUsernameProvider();

    // if authentication == null no exception is raised
    assertNull(usernameProvider.apply(null));

    {
      OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
      DefaultOidcUser oidcUser = mock(DefaultOidcUser.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);
      when(oidcUser.getEmail()).thenReturn(username);

      // authentication instance of OAuth2AuthenticationToken
      assertEquals(username, usernameProvider.apply(auth));
    }

    {
      // authentication instance of UsernamePasswordAuthenticationToken
      UsernamePasswordAuthenticationToken passwordAuthenticationToken =
          mock(UsernamePasswordAuthenticationToken.class);
      CrowdUserDetails userDetails = mock(CrowdUserDetails.class);
      when(passwordAuthenticationToken.getPrincipal()).thenReturn(userDetails);
      when(userDetails.getUsername()).thenReturn(username);
      assertEquals(username, usernameProvider.apply(passwordAuthenticationToken));
    }

    {
      // authentication instance of something else
      assertNull(usernameProvider.apply(mock(Authentication.class)));
    }
  }
}
