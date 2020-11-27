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
package org.opendevstack.provision.authentication.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendevstack.provision.authentication.authorization.ProvAppExpressionHandler;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DirtiesContext
public class ProvAppExpressionHandlerTest {

  @MockBean private Authentication authentication;

  private static final String USER_ROLE = "userRole";
  private static final String ADMIN_ROLE = "adminRole";

  private ProvAppExpressionHandler provAppExpressionHandler;

  @BeforeEach
  public void setup() {
    provAppExpressionHandler = new ProvAppExpressionHandler(USER_ROLE, ADMIN_ROLE);
  }

  @Test
  public void createEvaluationContextInternal() throws NoSuchMethodException {
    MethodInvocation mi = mock(MethodInvocation.class);
    when(mi.getMethod()).thenReturn(String.class.getMethod("toString"));
    when(mi.getThis()).thenReturn(this);

    StandardEvaluationContext evaluationContextInternal =
        provAppExpressionHandler.createEvaluationContextInternal(authentication, mi);
    ProvAppExpressionHandler handler =
        (ProvAppExpressionHandler)
            evaluationContextInternal.lookupVariable(ProvAppExpressionHandler.PROV_APP);
    assertEquals(provAppExpressionHandler, handler);
  }

  @Test
  public void adminRole() {
    assertEquals(ADMIN_ROLE, provAppExpressionHandler.adminRole());
  }

  @Test
  public void userRole() {
    assertEquals(USER_ROLE, provAppExpressionHandler.userRole());
  }

  @Test
  public void allRoles() {
    assertTrue(provAppExpressionHandler.allRoles().contains(USER_ROLE));
    assertTrue(provAppExpressionHandler.allRoles().contains(ADMIN_ROLE));
    assertEquals(2, provAppExpressionHandler.allRoles().size());

    try {
      // test that all roles list cannot be modified!
      provAppExpressionHandler.allRoles().add("new_role");
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }
}
