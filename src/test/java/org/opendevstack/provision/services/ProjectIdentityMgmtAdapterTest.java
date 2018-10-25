/*
 * Copyright 2018 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.InvalidGroupException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.integration.soap.SOAPGroup;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;

/**
 * @author utschig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class ProjectIdentityMgmtAdapterTest {

	@Mock
	CustomAuthenticationManager manager;

    @Autowired
    @InjectMocks
    ProjectIdentityMgmtAdapter idMgr;

    @Before
    public void initTests() {
      MockitoAnnotations.initMocks(this);
      SecurityServerClient client = Mockito.mock(SecurityServerClient.class);
	  Mockito.when(manager.getSecurityServerClient()).thenReturn(client);
    }
    
	@Test
	public void testGroupExists() throws Exception
	{
		SOAPGroup group = new SOAPGroup("xxx", null);
	    Mockito.when(manager.getSecurityServerClient().findGroupByName(group.getName())).thenReturn(group);
	    
	    assertTrue(idMgr.groupExists(group.getName()));

	    Mockito.when(manager.getSecurityServerClient().findGroupByName(group.getName())).thenReturn(null);
	    assertTrue(idMgr.groupExists(group.getName()));
	    
	    Mockito.when(manager.getSecurityServerClient().findGroupByName(group.getName())).
	    	thenThrow(new GroupNotFoundException("GroupNotFound"));
	    assertFalse(idMgr.groupExists(group.getName()));
	}


	@Test
	public void testUserExists() throws Exception
	{
		SOAPPrincipal principal = new SOAPPrincipal("user");
	    Mockito.when(manager.getSecurityServerClient().findPrincipalByName(principal.getName())).thenReturn(principal);
	    
	    assertTrue(idMgr.userExists(principal.getName()));

	    Mockito.when(manager.getSecurityServerClient().findPrincipalByName(principal.getName())).thenReturn(null);
	    assertTrue(idMgr.userExists(principal.getName()));
	    
	    Mockito.when(manager.getSecurityServerClient().findPrincipalByName(principal.getName())).
	    	thenThrow(new UserNotFoundException(principal.getName()));
		assertFalse(idMgr.userExists(principal.getName()));
	}

	@Test
	public void testCreateGroup () throws Exception 
	{
		SOAPGroup group = new SOAPGroup("xxx", null);

		Mockito.when(manager.getSecurityServerClient().addGroup(group)).thenReturn(group);
		assertEquals(group.getName(), idMgr.createGroupInternal(group.getName()));
	}

	
	@Test (expected = IdMgmtException.class)
	public void testCreateNullGroup () throws Exception 
	{
		idMgr.createGroupInternal(null);
	}
	
	@Test (expected = IdMgmtException.class)
	public void testCreateGroupSOAPErr () throws Exception 
	{
		SOAPGroup group = new SOAPGroup("xxx", null);

		Mockito.when(manager.getSecurityServerClient().addGroup(group)).thenThrow(InvalidGroupException.class);
		idMgr.createGroupInternal(group.getName());
	}
	
	
	@Test
	public void testValidateProject () throws Exception
	{
		SOAPPrincipal principal = new SOAPPrincipal("user");
	    Mockito.when(manager.getSecurityServerClient().findPrincipalByName(principal.getName())).thenReturn(principal);
	    
		SOAPGroup group = new SOAPGroup("xxx", null);
	    Mockito.when(manager.getSecurityServerClient().findGroupByName(group.getName())).thenReturn(group);
	    
	    ProjectData data = new ProjectData();
	    data.adminGroup = group.getName();
	    data.userGroup = group.getName();
	    data.readonlyGroup = group.getName();
	    data.admin = principal.getName();
	    
	    idMgr.validateIdSettingsOfProject(data);
	    
	    data.userGroup = "doesNotExist";
	    Mockito.when(manager.getSecurityServerClient().findGroupByName(data.userGroup)).
	    	thenThrow(new GroupNotFoundException(""));

	    Exception testE = null;
	    try 
	    {
	    	idMgr.validateIdSettingsOfProject(data);
	    } catch (IdMgmtException idEx) {
	    	testE = idEx;
	    }
	    assertNotNull(testE);
	    assertTrue (testE.getMessage().contains(data.userGroup));
	    
	    Mockito.when(manager.getSecurityServerClient().findPrincipalByName(principal.getName())).
    		thenThrow(new UserNotFoundException(principal.getName()));

	    testE = null;
	    try 
	    {
	    	idMgr.validateIdSettingsOfProject(data);
	    } catch (IdMgmtException idEx) {
	    	testE = idEx;
	    }
	    assertNotNull(testE);
	    assertTrue (testE.getMessage().contains(data.userGroup));
	    assertTrue (testE.getMessage().contains(data.admin));
	    
	}
}
