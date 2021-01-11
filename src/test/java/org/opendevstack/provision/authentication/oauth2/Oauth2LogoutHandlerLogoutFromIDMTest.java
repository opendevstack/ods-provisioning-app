package org.opendevstack.provision.authentication.oauth2;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Oauth2LogoutHandler.class)
@TestPropertySource(
    properties = {
      "provision.auth.provider=oauth2",
      "idmanager.url=url",
      "idmanager.realm=realm",
      "idmanager.disable-logout-from-idm=false"
    })
public class Oauth2LogoutHandlerLogoutFromIDMTest {

  @Autowired Oauth2LogoutHandler logoutHandler;

  @MockBean private HttpServletRequest request;

  @MockBean private HttpServletResponse response;

  @MockBean private Authentication authentication;

  @Value("${idmanager.url}")
  private String idManagerUrl;

  @Value("${idmanager.realm}")
  private String idManagerRealm;

  @Test
  public void whenLogoutFromIDMIsDisableThenRedirectToLogoutPath() throws IOException {

    when(request.getScheme()).thenReturn("scheme");
    when(request.getServerName()).thenReturn("name");
    when(request.getServerPort()).thenReturn(8000);
    when(request.getContextPath()).thenReturn("context");
    when(response.encodeRedirectURL(anyString())).thenReturn("encoded");

    logoutHandler.logout(request, response, authentication);

    verify(response, times(1)).sendRedirect(contains(idManagerUrl));
    verify(response, times(1)).sendRedirect(contains(idManagerRealm));
  }
}
