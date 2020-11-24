/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendevstack.provision.authentication.crowd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;
import com.atlassian.crowd.model.authentication.ValidationFactor;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import com.atlassian.crowd.service.soap.client.SecurityServerClientImpl;
import com.atlassian.crowd.service.soap.client.SoapClientProperties;
import com.atlassian.crowd.service.soap.client.SoapClientPropertiesImpl;
import java.rmi.RemoteException;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendevstack.provision.SpringBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
@WithMockUser(
    username = CrowdAuthenticationManagerTest.USER,
    roles = {"ADMIN"})
@ActiveProfiles("crowd")
public class CrowdAuthenticationManagerTest {

  private static final String TOKEN = "token";

  /** This is for tests only */
  static final String USER = "test";

  @SuppressWarnings("squid:S2068")
  static final String TEST_CRED = "test";

  @Autowired CrowdAuthenticationManager manager;

  @Mock private SecurityServerClient securityServerClient;

  @Before
  public void setUp() throws Exception {
    String token = TOKEN;
    ValidationFactor[] factors = new ValidationFactor[0];
    Mockito.when(securityServerClient.isValidToken("", factors)).thenReturn(true);

    Mockito.doThrow(new RemoteException()).when(securityServerClient).invalidateToken("remote");
    Mockito.doThrow(new InvalidAuthorizationTokenException())
        .when(securityServerClient)
        .invalidateToken("invalid_token");
    Mockito.doThrow(new InvalidAuthenticationException(token))
        .when(securityServerClient)
        .invalidateToken("invalid_auth");
    manager.setSecurityServerClient(securityServerClient);
  }

  @Test
  public void setUserPassword() throws Exception {
    String pass = TEST_CRED;

    manager.setUserPassword(pass);

    assertEquals(pass, manager.getUserPassword());
  }

  @Test
  public void authenticateWithContext() throws Exception {
    SecurityServerClient client = Mockito.mock(SecurityServerClientImpl.class);
    Mockito.when(client.authenticatePrincipal(getContext())).thenReturn(null);
    Mockito.when(client.getSoapClientProperties()).thenReturn(getProps());

    manager.setSecurityServerClient(client);

    assertNull(manager.authenticate(getContext()));
  }

  @Test
  public void authenticateWithoutValidatingPassword() throws Exception {
    SecurityServerClient client = Mockito.mock(SecurityServerClientImpl.class);
    Mockito.when(client.authenticatePrincipal(getContext())).thenReturn(null);
    Mockito.when(client.getSoapClientProperties()).thenReturn(getProps());

    manager.setSecurityServerClient(client);

    assertNull(manager.authenticateWithoutValidatingPassword(getContext()));
  }

  @Test
  public void authenticateWithUsernameAndPassword() throws Exception {
    SecurityServerClient client = Mockito.mock(SecurityServerClientImpl.class);
    Mockito.when(client.authenticatePrincipalSimple(USER, TEST_CRED)).thenReturn("login");

    manager.setSecurityServerClient(client);

    assertEquals("login", manager.authenticate(USER, TEST_CRED));
  }

  @Test
  public void isAuthenticated() throws Exception {
    assertTrue(manager.isAuthenticated("", new ValidationFactor[0]));
  }

  @Test(expected = RemoteException.class)
  public void invalidateWithRemoteException() throws Exception {
    manager.invalidate("remote");
  }

  @Test(expected = InvalidAuthorizationTokenException.class)
  public void invalidateWithInvalidAuthorizationTokenException() throws Exception {
    manager.invalidate("invalid_token");
  }

  @Test(expected = InvalidAuthenticationException.class)
  public void invalidateWithInvalidAuthenticationException() throws Exception {
    manager.invalidate("invalid_auth");
  }

  @Test
  public void getSecurityServerClient() throws Exception {
    assertNotNull(manager.getSecurityServerClient());
    assertTrue((manager.getSecurityServerClient() instanceof SecurityServerClient));
  }

  private UserAuthenticationContext getContext() {
    UserAuthenticationContext context = new UserAuthenticationContext();
    context.setName(USER);
    context.setValidationFactors(new ValidationFactor[0]);
    PasswordCredential credential = new PasswordCredential(USER);
    context.setCredential(credential);
    return context;
  }

  private SoapClientProperties getProps() {
    Properties plainProps = new Properties();
    plainProps.setProperty("application.name", USER);
    SoapClientProperties props = SoapClientPropertiesImpl.newInstanceFromProperties(plainProps);
    return props;
  }
}
