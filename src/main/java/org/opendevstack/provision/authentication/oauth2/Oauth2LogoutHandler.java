package org.opendevstack.provision.authentication.oauth2;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Logout Handler for OAUTH2. Redirects the caller to the identity managers
 * <em>openid-connect/logout</em> url, so the active user session is logged out.
 */
@Component
@ConditionalOnProperty(name = "provision.auth.provider", havingValue = "oauth2")
public class Oauth2LogoutHandler implements LogoutHandler {

  private static final Logger LOG = LoggerFactory.getLogger(Oauth2LogoutHandler.class);

  public static final String LOGOUT_PATH = Oauth2SecurityConfiguration.LOGIN_URI + "?logout";

  @Value("${idmanager.url}")
  private String idManagerUrl;

  @Value("${idmanager.realm}")
  private String idManagerRealm;

  @Value("${idmanager.disable-logout-from-idm:false}")
  private boolean disableRedirectLogoutToIdentityManager;

  public Oauth2LogoutHandler() {
    LOG.info(
        "Logout from identity manager is {}!",
        disableRedirectLogoutToIdentityManager ? "enabled" : "disabled");
  }

  @Override
  public void logout(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      Authentication authentication) {
    try {

      if (disableRedirectLogoutToIdentityManager) {

        httpServletResponse.sendRedirect(LOGOUT_PATH);

      } else {

        String redirectUri = buildRedirectUri(httpServletRequest, httpServletResponse);

        String logoutUrl =
            idManagerUrl
                + "/auth/realms/"
                + idManagerRealm
                + "/protocol/openid-connect/logout?redirect_uri="
                + redirectUri;
        httpServletResponse.sendRedirect(logoutUrl);
      }

    } catch (IOException e) {
      LOG.warn("Cannot send redirect", e);
    }
  }

  /**
   * Builds the uri that is used from the identity manager to redirect the call after the logout url
   * was called from the provision app
   *
   * @param httpServletRequest servlet request
   * @param httpServletResponse servlet response
   * @return the redirect uri
   */
  private String buildRedirectUri(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
    StringBuilder url = new StringBuilder();
    url.append(httpServletRequest.getScheme())
        .append("://")
        .append(httpServletRequest.getServerName());
    final int serverPort = httpServletRequest.getServerPort();
    if (serverPort != 80 && serverPort != 443) {
      url.append(":").append(serverPort);
    }
    url.append(httpServletRequest.getContextPath()).append("/login");
    return httpServletResponse.encodeRedirectURL(url.toString());
  }
}
