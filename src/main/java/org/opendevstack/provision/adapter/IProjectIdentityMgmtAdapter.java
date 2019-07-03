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
package org.opendevstack.provision.adapter;

import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.RundeckAdapter;

/**
 * Generic identity mgmt adapter - called to create the necessary ID groups used
 * later in {@link JiraAdapter}, {@link ConfluenceAdapter} and
 * {@link RundeckAdapter}
 * 
 * In case authentication is needed - manager gives you the username and
 * principal the password <code>
 * CrowdUserDetails userDetails =
 *       (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
 * 
 * '@'Autowired
 * CustomAuthenticationManager manager;
 * </code>
 * 
 * @author utschig
 */
public interface IProjectIdentityMgmtAdapter extends IServiceAdapter
{

    /**
     * Verify if a given group exists - in case it's passed down from the initial
     * screen
     * 
     * @param groupName
     * @return true in case the group exists otherwise false
     */
    public boolean groupExists(String groupName);

    /**
     * Verify if a given user exists - in case it's passed down from the initial
     * screen
     * 
     * @param userName
     * @return true in case the user exists otherwise false
     */
    public boolean userExists(String userName);

    /**
     * Create the users group
     * 
     * @param projectName
     *            the name of the project
     * @return the user groups name to be configured into artifacts
     * @throws Exception
     *             in case something goes wrong
     */
    public String createUserGroup(String projectName)
            throws IdMgmtException;

    /**
     * Create the admin group
     * 
     * @param projectName
     *            the name of the project
     * @return the admin groups name to be configured into artifacts
     * @throws Exception
     *             in case something goes wrong
     */
    public String createAdminGroup(String projectName)
            throws IdMgmtException;

    /**
     * Create the readonly group
     * 
     * @param projectName
     *            the name of the project
     * @return the admin groups name to be configured into artifacts
     * @throws Exception
     *             in case something goes wrong
     */
    public String createReadonlyGroup(String projectName)
            throws IdMgmtException;

    /**
     * Validate project group settings
     * 
     * @param project
     *            the project with the groups set
     * @throws Exception
     *             in case one or more groups cannot be found
     */
    public void validateIdSettingsOfProject(OpenProjectData project)
            throws IdMgmtException;

}