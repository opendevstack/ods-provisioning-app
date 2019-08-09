package org.opendevstack.provision.authentication.oauth2;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class Oauth2LogoutHandler implements LogoutHandler {

  @Autowired
  private Oauth2AuthenticationManager oauth2AuthenticationManager;

  private static final Logger LOG = LoggerFactory.getLogger(Oauth2LogoutHandler.class);

  @Value("${idmanager.url}")
  private String idManagerUrl;

  @Override
  public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
      Authentication authentication) {
    try {

      // Reconstruct original requesting URL
      StringBuilder url = new StringBuilder();
      url.append(httpServletRequest.getScheme()).append("://").append(
          httpServletRequest.getServerName());
      final int serverPort = httpServletRequest.getServerPort();
      if (serverPort != 80 && serverPort != 443) {
        url.append(":").append(serverPort);
      }
      url.append(httpServletRequest.getContextPath()).append("/login");
      String redirectParam =
          "redirect_uri=" + httpServletResponse.encodeRedirectURL(url.toString());

      String logoutUrl =
          idManagerUrl + "/auth/realms/master/protocol/openid-connect/logout?" + redirectParam;
      httpServletResponse.sendRedirect(logoutUrl);
    } catch (IOException e) {
      LOG.warn("Cannot send redirect", e);
    }
  }
}
