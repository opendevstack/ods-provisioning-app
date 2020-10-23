package org.opendevstack.provision.authentication;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
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
              Assert.assertEquals(authentication, auth);
              latch.countDown();
              return "username";
            });

    provAppHttpSessionListener.sessionDestroyed(sessionEvent);

    boolean await = latch.await(100, TimeUnit.MILLISECONDS);
    Assert.assertTrue(await);
    Assert.assertEquals(0, latch.getCount());
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
}
