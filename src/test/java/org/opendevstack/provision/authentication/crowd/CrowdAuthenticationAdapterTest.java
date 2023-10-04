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

import static org.junit.jupiter.api.Assertions.*;

import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.model.authentication.Session;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;
import com.atlassian.crowd.model.authentication.ValidationFactor;
import com.atlassian.crowd.service.client.CrowdClient;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@WithMockUser(
    username = CrowdAuthenticationAdapterTest.TEST_USER,
    roles = {"ADMIN"})
@ActiveProfiles("crowd")
public class CrowdAuthenticationAdapterTest {

  private static final String TOKEN = "token";

  /** This is for tests only */
  static final String TEST_USER = "test";

  @SuppressWarnings("squid:S2068")
  static final String TEST_CRED = "test";

  @Autowired private CrowdAuthenticationAdapter manager;

  @Mock private CrowdClient crowdClient;

  @BeforeEach
  public void setUp() throws Exception {
    String token = TOKEN;
    List<ValidationFactor> factors = new ArrayList<>();
    Session testSession = createTestSession();
    Mockito.when(crowdClient.validateSSOAuthenticationAndGetSession("", factors))
        .thenReturn(testSession);

    Mockito.doThrow(new ApplicationPermissionException())
        .when(crowdClient)
        .invalidateSSOToken("permission_exception");
    Mockito.doThrow(new OperationFailedException())
        .when(crowdClient)
        .invalidateSSOToken("operation_failed");
    Mockito.doThrow(new InvalidAuthenticationException(token))
        .when(crowdClient)
        .invalidateSSOToken("invalid_auth");
    manager.setCrowdClient(crowdClient);
  }

  private Session createTestSession() {
    return new Session() {

      Principal principal =
          new Principal() {
            @Override
            public String getName() {
              return TEST_USER;
            }
          };

      @Override
      public String getToken() {
        return "test_token";
      }

      @Override
      public Date getCreatedDate() {
        return null;
      }

      @Override
      public Date getExpiryDate() {
        return null;
      }

      @Override
      public Principal getUser() {
        return principal;
      }
    };
  }

  @Test
  public void setUserPassword() throws Exception {
    String pass = TEST_CRED;

    manager.setUserPassword(pass);

    assertEquals(pass, manager.getUserPassword());
  }

  @Test
  public void invalidateWithApplicationPermissionException() {
    assertThrows(
        ApplicationPermissionException.class, () -> manager.invalidate("permission_exception"));
  }

  @Test
  public void invalidateWithOperationFailedException() {
    assertThrows(OperationFailedException.class, () -> manager.invalidate("operation_failed"));
  }

  @Test
  public void invalidateWithInvalidAuthenticationException() {
    assertThrows(InvalidAuthenticationException.class, () -> manager.invalidate("invalid_auth"));
  }

  @Test
  public void getCrowdClient() {
    assertNotNull(manager.getCrowdClient());
    assertTrue((manager.getCrowdClient() instanceof CrowdClient));
  }

  private UserAuthenticationContext getContext() {
    UserAuthenticationContext context = new UserAuthenticationContext();
    context.setName(TEST_USER);
    context.setValidationFactors(new ValidationFactor[0]);
    PasswordCredential credential = new PasswordCredential(TEST_CRED);
    context.setCredential(credential);
    return context;
  }
}
