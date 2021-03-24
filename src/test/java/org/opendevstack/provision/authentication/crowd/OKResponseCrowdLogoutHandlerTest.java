package org.opendevstack.provision.authentication.crowd;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.atlassian.crowd.integration.http.HttpAuthenticator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class OKResponseCrowdLogoutHandlerTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private HttpAuthenticator authenticator;
  @Mock private Authentication authentication;

  @Test
  void logout() {

    OKResponseCrowdLogoutHandler handler = new OKResponseCrowdLogoutHandler();
    handler.setHttpAuthenticator(authenticator);

    handler.logout(request, response, authentication);

    verify(response, times(1)).setStatus(HttpServletResponse.SC_OK);
  }
}
