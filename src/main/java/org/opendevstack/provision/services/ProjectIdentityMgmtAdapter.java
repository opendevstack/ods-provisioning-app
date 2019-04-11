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

import java.util.HashMap;
import java.util.Map;

import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.integration.soap.SOAPGroup;

/**
 * Identity mgmt adapter to create / validate groups
 * @author utschig
 *
 */
@Service
public class ProjectIdentityMgmtAdapter implements IProjectIdentityMgmtAdapter  
{
    @Value("${crowd.user.group:}")
    private String crowdUserGroup;
	  
	@Value("${crowd.admin.group:}")
	private String crowdAdminGroup;
	
    private static final Logger logger = LoggerFactory.getLogger(ProjectIdentityMgmtAdapter.class);
	
	@Autowired
	CustomAuthenticationManager manager;
	 
    @Value("${crowd.local.directory}")
	String crowdLocalDirectory;

    public void validateIdSettingsOfProject (ProjectData project) throws IdMgmtException 
    {
    	Map<String, String> projectCheckStatus = new HashMap<String, String>();
    	
		long startTime = System.currentTimeMillis();

    	if (!groupExists(project.adminGroup)) {
    		projectCheckStatus.put("adminGroup", project.adminGroup);
    	}
    	if (!groupExists(project.userGroup)) {
    		projectCheckStatus.put("userGroup", project.userGroup);
    	}
    	if (!groupExists(project.readonlyGroup)) {
    		projectCheckStatus.put("readonlyGroup", project.readonlyGroup);
    	}
    	if (!userExists(project.admin)) {
    		projectCheckStatus.put("admin", project.admin);
    	}

    	logger.debug("identityCheck Name took (ms): " + 
				(System.currentTimeMillis() - startTime));
    	
    	if (!projectCheckStatus.isEmpty()) {
    		throw new IdMgmtException ("Identity check failed - these groups don't exist! " 
    				+ projectCheckStatus);
    	}
    }
    
	@Override
	@SuppressWarnings("squid:S1193")
	public boolean groupExists(String groupName) 
	{
		if (groupName == null || groupName.trim().length() == 0) 
			return true;

		long startTime = System.currentTimeMillis();
		try 
		{
			manager.getSecurityServerClient().findGroupByName(groupName);
			return true;
		} catch (Exception eSecurity) 
		{
			if (!(eSecurity instanceof GroupNotFoundException)) {
				logger.error("GroupFind call failed with: {}", eSecurity);
			}
			return false;
		} finally 
		{
			logger.debug("findGroupByName by Name took (ms): " + 
				(System.currentTimeMillis() - startTime));
		}
	}

	@Override
	@SuppressWarnings("squid:S1193")
	public boolean userExists(String userName) 
	{
		if (userName == null || userName.trim().length() == 0) 
			return true;
		
		long startTime = System.currentTimeMillis();
		try 
		{
			manager.getSecurityServerClient().findPrincipalByName(userName);
			return true;
		} catch (Exception eSecurity) 
		{
			if (!(eSecurity instanceof UsernameNotFoundException)) {
				logger.error("UserFind call failed with: {}", eSecurity);
			}
			return false;
		} finally 
		{
			logger.debug("findPrincipal by Name took (ms): " + 
				(System.currentTimeMillis() - startTime));
		}
	}

	@Override
	public String createUserGroup(String projectName) throws IdMgmtException 
	{
		return createGroupInternal(projectName);
	}


	@Override
	public String createAdminGroup(String projectName) throws IdMgmtException {
		return createGroupInternal(projectName);
	}

	@Override
	public String createReadonlyGroup(String projectName) throws IdMgmtException 
	{
		return createGroupInternal(projectName);
	}
		
	String createGroupInternal (String groupName) throws IdMgmtException {
		if (groupName == null || groupName.trim().length() == 0)
			throw new IdMgmtException ("Cannot create a null group!");
		
		try 
		{
			return manager.getSecurityServerClient().
				addGroup(new SOAPGroup(groupName, new String [] {})).getName();
		} catch (Exception eAddGroup) 
		{
			logger.error("Could not create group {}, error: {}", groupName, eAddGroup);
			throw new IdMgmtException(eAddGroup);
		}
	}
}
