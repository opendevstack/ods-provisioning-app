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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.atlassian.crowd.integration.soap.SOAPGroup;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.model.OpenProjectData;

/**
 * @author utschig
 * @author Stefan Lack
 */
@RunWith(MockitoJUnitRunner.class)
public class CrowdProjectIdentityMgmtAdapterTest {

  @Mock CrowdAuthenticationManager manager;

  @InjectMocks CrowdProjectIdentityMgmtAdapter idMgr;

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

  @Test(expected = IdMgmtException.class)
  public void testCreateNullGroup() throws Exception {
    idMgr.createGroupInternal(null);
  }

  @Test(expected = IdMgmtException.class)
  public void testCreateGroupSOAPErr() throws Exception {
    SOAPGroup group = new SOAPGroup("xxx", null);

    when(manager.addGroup(group.getName())).thenThrow(IdMgmtException.class);
    idMgr.createGroupInternal(group.getName());
  }

  @Test
  public void testValidateProject() throws Exception {
    SOAPPrincipal principal = mockPrincipalExists("user", true);
    SOAPGroup group = mockGroupExists("xxx", true);

    OpenProjectData data = new OpenProjectData();
    data.projectAdminGroup = group.getName();
    data.projectUserGroup = group.getName();
    data.projectReadonlyGroup = group.getName();
    data.projectAdminUser = principal.getName();

    idMgr.validateIdSettingsOfProject(data);

    data.projectUserGroup = "doesNotExistUG";
    data.projectAdminGroup = "doesNotExistAD";
    data.projectReadonlyGroup = "doesNotExistRO";

    mockGroupExists(data.projectUserGroup, false);
    mockGroupExists(data.projectAdminGroup, false);
    mockGroupExists(data.projectReadonlyGroup, false);

    Exception testE = null;
    try {
      idMgr.validateIdSettingsOfProject(data);
    } catch (IdMgmtException idEx) {
      testE = idEx;
    }
    assertNotNull(testE);
    assertTrue(testE.getMessage().contains(data.projectUserGroup));
    assertTrue(testE.getMessage().contains(data.projectAdminGroup));
    assertTrue(testE.getMessage().contains(data.projectReadonlyGroup));

    mockPrincipalExists(principal.getName(), false);
    testE = null;
    try {
      idMgr.validateIdSettingsOfProject(data);
    } catch (IdMgmtException idEx) {
      testE = idEx;
    }
    assertNotNull(testE);
    assertTrue(testE.getMessage().contains(data.projectUserGroup));
    assertTrue(testE.getMessage().contains(data.projectAdminUser));
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
