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
package org.opendevstack.provision.controller;

import java.util.HashMap;
import java.util.Map;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.authentication.PreAuthorizeAllRoles;
import org.opendevstack.provision.authentication.UserRolesHolder;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.services.StorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller to handle the process of project creation
 */
@RestController
@RequestMapping(value = ApplicationInfoAPI.APP_INFO_API_V2)
public class ApplicationInfoAPIController {

  private static Logger logger = LoggerFactory.getLogger(ApplicationInfoAPIController.class);

  @Autowired private StorageAdapter storageAdapter;

  @Autowired private UserRolesHolder userRolesHolder;

  @Autowired private IODSAuthnzAdapter manager;

  @Autowired private IBugtrackerAdapter jiraAdapter;

  @Autowired private ISCMAdapter bitbucketAdapter;

  @Value("${adapters.confluence.enabled:true}")
  private boolean confluenceAdapterEnable;

  @Value("${idmanager.group.opendevstack-users}")
  private String idmanagerUserGroup;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String idmanagerAdminGroup;

  @Autowired(required = false)
  private ICollaborationAdapter confluenceAdapter;

  @PreAuthorizeAllRoles
  @GetMapping(ApplicationInfoAPI.ENDPOINT_ABOUT)
  public @ResponseBody ResponseEntity<Map<String, Object>> about() {

    Map<String, Object> about = new HashMap<>();

    about.put("username", manager.getUserName());
    about.put("email", manager.getUserEmail());
    about.put("userRoles", userRolesHolder.getUserRoles());

    // add endpoint map
    Map<String, String> endpoints = new HashMap<>();
    endpoints.put("JIRA", jiraAdapter.getAdapterApiUri());
    endpoints.put("GIT", bitbucketAdapter.getAdapterApiUri());

    if (confluenceAdapterEnable) {
      endpoints.put("CONFLUENCE", confluenceAdapter.getAdapterApiUri());
    } else {
      endpoints.put("CONFLUENCE", "no project created (confluence is disabled by configuration!)");
    }

    about.put("endpoints", endpoints);

    about.put("idmanagerUserGroup", idmanagerUserGroup.toLowerCase());
    about.put("idmanagerAdminGroup", idmanagerAdminGroup.toLowerCase());

    return ResponseEntity.ok(about);
  }

  @PreAuthorizeAllRoles
  @GetMapping(ApplicationInfoAPI.ENDPOINT_HISTORY)
  public ResponseEntity<Map<String, Map<String, OpenProjectInfo>>> history() {

    Map<String, Map<String, OpenProjectInfo>> history = new HashMap<>();

    Map<String, OpenProjectData> projectHistoryMap = storageAdapter.listProjectHistory();
    Map<String, OpenProjectInfo> projectInfoMap = new HashMap<>(projectHistoryMap.size());
    for (String key : projectHistoryMap.keySet()) {
      projectInfoMap.put(key, new OpenProjectInfo(projectHistoryMap.get(key)));
    }

    history.put("projectHistory", projectInfoMap);

    return ResponseEntity.ok(history);
  }
}
