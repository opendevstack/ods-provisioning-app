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

import java.util.HashMap;
import java.util.Map;
import org.opendevstack.provision.adapter.IProjectIdentityMgmtAdapter;
import org.opendevstack.provision.adapter.exception.IdMgmtException;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.OpenProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.integration.soap.SOAPGroup;

/**
 * Identity mgmt adapter to create / validate groups
 * 
 * @author utschig
 *
 */
@Service
public class CrowdProjectIdentityMgmtAdapter implements IProjectIdentityMgmtAdapter {
  private static final Logger logger =
      LoggerFactory.getLogger(CrowdProjectIdentityMgmtAdapter.class);

  @Autowired
  CustomAuthenticationManager manager;

  public void validateIdSettingsOfProject(OpenProjectData project) throws IdMgmtException {
    Map<String, String> projectCheckStatus = new HashMap<>();

    long startTime = System.currentTimeMillis();

    if (!groupExists(project.projectAdminGroup)) {
      projectCheckStatus.put("adminGroup", project.projectAdminGroup);
    }
    if (!groupExists(project.projectUserGroup)) {
      projectCheckStatus.put("userGroup", project.projectUserGroup);
    }
    if (!groupExists(project.projectReadonlyGroup)) {
      projectCheckStatus.put("readonlyGroup", project.projectReadonlyGroup);
    }
    if (!userExists(project.projectAdminUser)) {
      projectCheckStatus.put("admin", project.projectAdminUser);
    }

    logger.debug("identityCheck Name took (ms): {}", System.currentTimeMillis() - startTime);

    if (!projectCheckStatus.isEmpty()) {
      throw new IdMgmtException(
          "Identity check failed - these groups don't exist! " + projectCheckStatus);
    }
  }

  @Override
  @SuppressWarnings("squid:S1193")
  public boolean groupExists(String groupName) {
    if (groupName == null || groupName.trim().length() == 0)
      return true;

    long startTime = System.currentTimeMillis();
    try {
      manager.getSecurityServerClient().findGroupByName(groupName);
      return true;
    } catch (Exception eSecurity) {
      if (!(eSecurity instanceof GroupNotFoundException)) {
        logger.error("GroupFind call failed with:", eSecurity);
      }
      return false;
    } finally {
      logger.debug("findGroupByName by Name took (ms): {}", System.currentTimeMillis() - startTime);
    }
  }

  @Override
  @SuppressWarnings("squid:S1193")
  public boolean userExists(String userName) {
    if (userName == null || userName.trim().length() == 0)
      return true;

    long startTime = System.currentTimeMillis();
    try {
      manager.getSecurityServerClient().findPrincipalByName(userName);
      return true;
    } catch (Exception eSecurity) {
      if (!(eSecurity instanceof UsernameNotFoundException)) {
        logger.error("UserFind call failed with:", eSecurity);
      }
      return false;
    } finally {
      logger.debug("findPrincipal by Name took (ms): {}", System.currentTimeMillis() - startTime);
    }
  }

  @Override
  public String createUserGroup(String projectName) throws IdMgmtException {
    return createGroupInternal(projectName);
  }

  @Override
  public String createAdminGroup(String projectName) throws IdMgmtException {
    return createGroupInternal(projectName);
  }

  @Override
  public String createReadonlyGroup(String projectName) throws IdMgmtException {
    return createGroupInternal(projectName);
  }

  String createGroupInternal(String groupName) throws IdMgmtException {
    if (groupName == null || groupName.trim().length() == 0)
      throw new IdMgmtException("Cannot create a null group!");

    try {
      return manager.getSecurityServerClient().addGroup(new SOAPGroup(groupName, new String[] {}))
          .getName();
    } catch (Exception eAddGroup) {
      logger.error("Could not create group {}, error: {}", groupName, eAddGroup);
      throw new IdMgmtException(eAddGroup);
    }
  }

  @Override
  public String getAdapterApiUri() {
    return manager.getSecurityServerClient().getSoapClientProperties().getBaseURL();
  }

  @Override
  public Map<String, String> getProjects(String filter) {
    return new HashMap<>();
  }

  @Override
  public Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(LIFECYCLE_STAGE stage,
      OpenProjectData project) {
    return new HashMap<>();
  }

}