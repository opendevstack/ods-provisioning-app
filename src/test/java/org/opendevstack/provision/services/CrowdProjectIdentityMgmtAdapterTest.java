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

package org.opendevstack.provision.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.soap.SOAPGroup;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.model.OpenProjectData;

@ExtendWith(MockitoExtension.class)
public class CrowdProjectIdentityMgmtAdapterTest {

  @InjectMocks private CrowdProjectIdentityMgmtAdapter idMgr;

  @Mock private IODSAuthnzAdapter manager;

  @Test
  public void testGroupExists() {
    SOAPGroup group = new SOAPGroup("xxx", null);
    when(manager.existsGroupWithName(eq(group.getName()))).thenReturn(true);

    assertTrue(idMgr.groupExists(group.getName()));

    when(manager.existsGroupWithName(eq(group.getName()))).thenReturn(false);
    assertFalse(idMgr.groupExists(group.getName()));
  }

  @Test
  public void testUserExists() {
    SOAPPrincipal principal = mockPrincipalExists("user", true);

    assertTrue(idMgr.userExists(principal.getName()));

    when(manager.existPrincipalWithName(principal.getName())).thenReturn(false);
    assertFalse(idMgr.userExists(principal.getName()));
  }

  @Test
  public void testCreateGroup() throws Exception {
    SOAPGroup group = new SOAPGroup("xxx", null);

    when(manager.addGroup(group.getName())).thenReturn(group.getName());
    String groupInternal = idMgr.createGroupInternal(group.getName());
    assertEquals(group.getName(), groupInternal);

    assertEquals(group.getName(), idMgr.createAdminGroup(group.getName()));
    assertEquals(group.getName(), idMgr.createUserGroup(group.getName()));
    assertEquals(group.getName(), idMgr.createReadonlyGroup(group.getName()));
  }

  @Test
  public void testCreateNullGroup() {
    assertThrows(IdMgmtException.class, () -> idMgr.createGroupInternal(null));
  }

  @Test
  public void testCreateGroupSOAPErr() {
    assertThrows(
        IdMgmtException.class,
        () -> {
          SOAPGroup group = new SOAPGroup("xxx", null);
          when(manager.addGroup(group.getName())).thenThrow(IdMgmtException.class);
          idMgr.createGroupInternal(group.getName());
        });
  }

  @Test
  public void testValidateProject() throws Exception {
    SOAPPrincipal principal = mockPrincipalExists("user", true);
    SOAPGroup group = mockGroupExists("xxx", true);

    OpenProjectData data = new OpenProjectData();
    data.setProjectAdminGroup(group.getName());
    data.setProjectUserGroup(group.getName());
    data.setProjectReadonlyGroup(group.getName());
    data.setProjectAdminUser(principal.getName());

    idMgr.validateIdSettingsOfProject(data);

    data.setProjectUserGroup("doesNotExistUG");
    data.setProjectAdminGroup("doesNotExistAD");
    data.setProjectReadonlyGroup("doesNotExistRO");

    mockGroupExists(data.getProjectUserGroup(), false);
    mockGroupExists(data.getProjectAdminGroup(), false);
    mockGroupExists(data.getProjectReadonlyGroup(), false);

    Exception testE = null;
    try {
      idMgr.validateIdSettingsOfProject(data);
    } catch (IdMgmtException idEx) {
      testE = idEx;
    }
    assertNotNull(testE);
    assertTrue(testE.getMessage().contains(data.getProjectUserGroup()));
    assertTrue(testE.getMessage().contains(data.getProjectAdminGroup()));
    assertTrue(testE.getMessage().contains(data.getProjectReadonlyGroup()));

    mockPrincipalExists(principal.getName(), false);
    testE = null;
    try {
      idMgr.validateIdSettingsOfProject(data);
    } catch (IdMgmtException idEx) {
      testE = idEx;
    }
    assertNotNull(testE);
    assertTrue(testE.getMessage().contains(data.getProjectUserGroup()));
    assertTrue(testE.getMessage().contains(data.getProjectAdminUser()));
  }

  public SOAPGroup mockGroupExists(String groupName, boolean existsGroup) {
    SOAPGroup group = new SOAPGroup(groupName, null);
    when(manager.existsGroupWithName(group.getName())).thenReturn(existsGroup);
    return group;
  }

  public SOAPPrincipal mockPrincipalExists(String user, boolean exists) {
    SOAPPrincipal principal = new SOAPPrincipal(user);
    when(manager.existPrincipalWithName(principal.getName())).thenReturn(exists);
    return principal;
  }
}
