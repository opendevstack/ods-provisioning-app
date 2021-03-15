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
  public static final String PROTOCOL_OPENID_CONNECT_LOGOUT_REDIRECT_URI =
      "/protocol/openid-connect/logout?redirect_uri=";

  private boolean spafrontendEnabled;

  private String idManagerUrl;

  private String idManagerRealm;

  private boolean logoutFromIDP;

  public Oauth2LogoutHandler() {
    LOG.info("Logout from identity manager is {}!", logoutFromIDP ? "enabled" : "disabled");
  }

  public Oauth2LogoutHandler(
      @Value("${frontend.spa.enabled:false}") boolean spafrontendEnabled,
      @Value("${idmanager.disable-logout-from-idm:false}") boolean logoutFromIDP,
      @Value("${idmanager.url}") String idManagerUrl,
      @Value("${idmanager.realm}") String idManagerRealm) {
    this.spafrontendEnabled = spafrontendEnabled;
    this.logoutFromIDP = logoutFromIDP;
    this.idManagerUrl = idManagerUrl;
    this.idManagerRealm = idManagerRealm;
  }

  @Override
  public void logout(
      HttpServletRequest httpServletRequest,
      HttpServletResponse response,
      Authentication authentication) {
    try {

      if (logoutFromIDP()) {
        response.sendRedirect(buildIDPLogoutUrl(httpServletRequest, response));

      } else if (spafrontendEnabled) {
        response.setStatus(HttpServletResponse.SC_OK);

      } else {
        response.sendRedirect(LOGOUT_PATH);
      }

    } catch (IOException e) {
      LOG.warn("Cannot send redirect", e);
    }
  }

  private boolean logoutFromIDP() {
    return logoutFromIDP;
  }

  /**
   * Builds the uri that is used from the identity manager to redirect the call after the logout url
   * was called from the provision app
   *
   * @param httpServletRequest servlet request
   * @param httpServletResponse servlet response
   * @return the redirect uri
   */
  private String buildIDPLogoutUrl(
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

    String redirectUri = httpServletResponse.encodeRedirectURL(url.toString());

    String logoutUrl =
        idManagerUrl
            + "/auth/realms/"
            + idManagerRealm
            + PROTOCOL_OPENID_CONNECT_LOGOUT_REDIRECT_URI
            + redirectUri;

    return logoutUrl;
  }
}
