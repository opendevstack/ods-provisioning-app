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
import java.util.List;
import java.util.Map;
import org.opendevstack.provision.adapter.IBugtrackerAdapter;
import org.opendevstack.provision.adapter.ICollaborationAdapter;
import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.services.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Default controller for navigating the page. Autowiring per setter because of testability
 *
 * @author Brokmeier, Pascal
 */
@Controller
public class DefaultController {
  StorageAdapter storageAdapter;

  IODSAuthnzAdapter manager;

  private IJobExecutionAdapter rundeckAdapter;

  private IBugtrackerAdapter jiraAdapter;

  private ISCMAdapter bitbucketAdapter;

  @Autowired
  private ICollaborationAdapter confluenceAdapter;

  private static final String LOGIN_REDIRECT = "redirect:/login";

  private static final String ACTIVE = "active";

  @Value("${crowd.user.group}")
  private String crowdUserGroup;

  @Value("${crowd.admin.group}")
  private String crowdAdminGroup;

  @Value("${openshift.project.upgrade}")
  private boolean ocUpgradeAllowed;

  @Autowired
  List<String> projectTemplateKeyNames;

  @RequestMapping("/")
  String rootRedirect() {
    return "redirect:/home";
  }

  @RequestMapping("/home")
  String home(Model model) {
    if (!isAuthenticated()) {
      return LOGIN_REDIRECT;
    }
    model.addAttribute("classActiveHome", ACTIVE);
    return "home";
  }

  @RequestMapping("/provision")
  String provisionProject(Model model) {
    if (!isAuthenticated()) {
      return LOGIN_REDIRECT;
    } else {
      model.addAttribute("jiraProjects", storageAdapter.listProjectHistory());
      model.addAttribute("quickStarter", rundeckAdapter.getQuickstarters());
      model.addAttribute("crowdUserGroup", crowdUserGroup.toLowerCase());
      model.addAttribute("crowdAdminGroup", crowdAdminGroup.toLowerCase());
      model.addAttribute("ocUpgradeAllowed", ocUpgradeAllowed);
      model.addAttribute("projectTypes", projectTemplateKeyNames);
    }
    model.addAttribute("classActiveNew", ACTIVE);
    return "provision";
  }

  @RequestMapping("/login")
  String login(Model model) {
    return "login";
  }

  @RequestMapping("/history")
  String history(Model model) {
    if (!isAuthenticated()) {
      return LOGIN_REDIRECT;
    }
    model.addAttribute("classActiveHistory", ACTIVE);
    model.addAttribute("projectHistory", storageAdapter.listProjectHistory());
    return "history";
  }

  @RequestMapping("/about")
  String about(Model model) {
    if (!isAuthenticated()) {
      return LOGIN_REDIRECT;
    }
    model.addAttribute("classActiveAbout", ACTIVE);
    model.addAttribute("aboutChanges", storageAdapter.listAboutChangesData().aboutDataList);

    // add endpoint map
    Map<String, String> endpoints = new HashMap<>();
    endpoints.put("JIRA", jiraAdapter.getAdapterApiUri());
    endpoints.put("GIT", bitbucketAdapter.getAdapterApiUri());
    endpoints.put("RUNDECK", rundeckAdapter.getAdapterApiUri());
    endpoints.put("CONFLUENCE", confluenceAdapter.getAdapterApiUri());

    model.addAttribute("endpointMap", endpoints);

    model.addAttribute("crowdUserGroup", crowdUserGroup.toLowerCase());
    model.addAttribute("crowdAdminGroup", crowdAdminGroup.toLowerCase());
    return "about";
  }

  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public String logoutPage() {
    try {
      manager.invalidateIdentity();
    } catch (Exception eAllLogout) {
    }
    return "redirect:/login?logout";
  }

  private boolean isAuthenticated() {
    return (manager.getUserPassword() != null);
  }

  @Autowired
  public void setCustomAuthenticationManager(IODSAuthnzAdapter manager) {
    this.manager = manager;
  }

  @Autowired
  public void setRundeckAdapter(IJobExecutionAdapter rundeckAdapter) {
    this.rundeckAdapter = rundeckAdapter;
  }

  @Autowired
  public void setStorageAdapter(StorageAdapter storageAdapter) {
    this.storageAdapter = storageAdapter;
  }

  @Autowired
  public void setBugTrackerAdapter(IBugtrackerAdapter jiraAdapter) {
    this.jiraAdapter = jiraAdapter;
  }

  @Autowired
  public void setSCMAdapter(ISCMAdapter bitbucketAdapter) {
    this.bitbucketAdapter = bitbucketAdapter;
  }
}
