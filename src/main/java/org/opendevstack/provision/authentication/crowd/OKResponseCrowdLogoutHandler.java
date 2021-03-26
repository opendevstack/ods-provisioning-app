package org.opendevstack.provision.authentication.crowd;

import com.atlassian.crowd.integration.springsecurity.CrowdLogoutHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

public class OKResponseCrowdLogoutHandler extends CrowdLogoutHandler {

  private static final Logger logger = LoggerFactory.getLogger(CrowdSecurityConfiguration.class);

  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    try {
      super.logout(request, response, authentication);
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (Exception ex) {
      logger.error("could not set status OK to response!", ex);
    }
  }
}
