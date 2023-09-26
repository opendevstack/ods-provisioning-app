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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.model.OpenProjectData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrowdProjectIdentityMgmtAdapterTest {

    @InjectMocks
    private CrowdProjectIdentityMgmtAdapter idMgr;

    @Mock
    private IODSAuthnzAdapter manager;

    @Test
    public void testGroupExists() {
        String group = "xxx";

        when(manager.existsGroupWithName(eq(group))).thenReturn(true);
        assertTrue(idMgr.groupExists(group));

        when(manager.existsGroupWithName(eq(group))).thenReturn(false);
        assertFalse(idMgr.groupExists(group));
    }

    @Test
    public void testUserExists() {
        String principal = "user";

        when(manager.existPrincipalWithName(principal)).thenReturn(true);
        assertTrue(idMgr.userExists(principal));

        when(manager.existPrincipalWithName(principal)).thenReturn(false);
        assertFalse(idMgr.userExists(principal));
    }

    @Test
    public void testCreateGroup() throws Exception {
        String group = "xxx";

        when(manager.addGroup(group)).thenReturn(group);
        String groupInternal = idMgr.createGroupInternal(group);
        assertEquals(group, groupInternal);

        assertEquals(group, idMgr.createAdminGroup(group));
        assertEquals(group, idMgr.createUserGroup(group));
        assertEquals(group, idMgr.createReadonlyGroup(group));
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
                    String group = "xxx";
                    when(manager.addGroup(group)).thenThrow(IdMgmtException.class);
                    idMgr.createGroupInternal(group);
                });
    }

    @Test
    public void testValidateProject() throws Exception {
        String principal = mockPrincipalExists("user", true);
        String group = mockGroupExists("xxx", true);

        OpenProjectData data = new OpenProjectData();
        data.setProjectAdminGroup(group);
        data.setProjectUserGroup(group);
        data.setProjectReadonlyGroup(group);
        data.setProjectAdminUser(principal);

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

        mockPrincipalExists(principal, false);
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

    public String mockGroupExists(String groupName, boolean existsGroup) {
        when(manager.existsGroupWithName(groupName)).thenReturn(existsGroup);
        return groupName;
    }

    public String mockPrincipalExists(String user, boolean exists) {
        when(manager.existPrincipalWithName(user)).thenReturn(exists);
        return user;
    }
}
