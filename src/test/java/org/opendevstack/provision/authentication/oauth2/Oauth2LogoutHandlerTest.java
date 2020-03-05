package org.opendevstack.provision.authentication.oauth2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@DirtiesContext
@ContextConfiguration(classes = Oauth2LogoutHandler.class)
@TestPropertySource(properties = {
        "provision.auth.provider=oauth2",
        "idmanager.url=url",
        "idmanager.realm=realm",
        "idmanager.disable-logout-from-idm=true"})
public class Oauth2LogoutHandlerTest {

    @Autowired
    Oauth2LogoutHandler logoutHandler;

    @MockBean
    private HttpServletRequest request;
    @MockBean
    private HttpServletResponse response;
    @MockBean
    private Authentication authentication;

    @Test
    public void whenLogoutFromIDMIsDisableThenRedirectToLogoutPath() throws IOException {

        logoutHandler.logout(request, response, authentication);

        verify(response, times(1)).sendRedirect("/login?logout");

    }

}
