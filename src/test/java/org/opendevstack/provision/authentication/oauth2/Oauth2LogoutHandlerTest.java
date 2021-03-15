package org.opendevstack.provision.authentication.oauth2;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class Oauth2LogoutHandlerTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private Authentication authentication;

  private String idManagerUrl = "testUrl";
  private String idManagerRealm = "testRealm";

  @Test
  public void whenLogoutFromIDMIsEnabled_thenRedirectToIDP() throws IOException {

    String idManagerUrl = "testUrl";

    String idManagerRealm = "testRealm";

    when(request.getScheme()).thenReturn("scheme");
    when(request.getServerName()).thenReturn("name");
    when(request.getServerPort()).thenReturn(8000);
    when(request.getContextPath()).thenReturn("context");
    when(response.encodeRedirectURL(anyString())).thenReturn("encoded");

    Oauth2LogoutHandler logoutHandler =
        new Oauth2LogoutHandler(false, true, idManagerUrl, idManagerRealm);

    logoutHandler.logout(request, response, authentication);

    verify(response, times(1))
        .sendRedirect(contains(Oauth2LogoutHandler.PROTOCOL_OPENID_CONNECT_LOGOUT_REDIRECT_URI));
    verify(response, times(1)).sendRedirect(contains(idManagerUrl));
    verify(response, times(1)).sendRedirect(contains(idManagerRealm));
  }

  @Test
  public void whenLogoutFromIDMIsDisabledAndClientIsNotSPA_thenRedirectToLogoutPath()
      throws IOException {

    Oauth2LogoutHandler logoutHandler =
        new Oauth2LogoutHandler(false, false, idManagerUrl, idManagerRealm);

    logoutHandler.logout(request, response, authentication);

    verify(response, times(1)).sendRedirect(Oauth2LogoutHandler.LOGOUT_PATH);
  }

  @Test
  public void whenLogoutFromIDMIsDisabledAndClientIsSPA_thenReturnOK() throws IOException {

    Oauth2LogoutHandler logoutHandler =
        new Oauth2LogoutHandler(true, false, idManagerUrl, idManagerRealm);

    logoutHandler.logout(request, response, authentication);

    verify(response, times(1)).setStatus(HttpServletResponse.SC_OK);
  }
}
