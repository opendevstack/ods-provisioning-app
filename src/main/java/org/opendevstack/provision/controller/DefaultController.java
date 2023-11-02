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

import java.util.*;
import org.opendevstack.provision.adapter.*;
import org.opendevstack.provision.services.StorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Default controller for navigating the page. Autowiring per setter because of testability */
@Controller
public class DefaultController {

  private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

  public static final String OAUTH_2_PROVIDER_KEY = "oauth2";

  public static final String ROUTE_ROOT = "/";
  public static final String ROUTE_HOME = "/home";
  public static final String ROUTE_PROVISION = "/provision";
  public static final String ROUTE_LOGIN = "/login";
  public static final String ROUTE_LOGIN_WITH_LOGOUT_OPTION = "/login?logout";
  public static final String ROUTE_HISTORY = "/history";
  public static final String ROUTE_ABOUT = "/about";
  public static final String ROUTE_LOGOUT = "/logout";
  public static final String ROUTE_ROOT_AND_ALL_RESOURCES_IN_PATH = "/{path:[^\\.]*}";
  public static final String FORWARD_TO_ROOT = "forward:/";
  public static final String RESOURCE_NFE_INDEX = "/nfe/index.html";

  public static final String REDIRECT_TO_HOME = "redirect:/home";
  public static final String REDIRECT_TO_SPA_FRONTEND_INDEX_HTML = "redirect:" + RESOURCE_NFE_INDEX;
  public static final String REDIRECT_TO_LOGIN = "redirect:/login";
  public static final String REDIRECT_TO_LOGIN_WITH_LOGOUT = "redirect:/login?logout";

  private StorageAdapter storageAdapter;

  private IODSAuthnzAdapter manager;

  private IJobExecutionAdapter jenkinspipelineAdapter;

  private IBugtrackerAdapter jiraAdapter;

  private ISCMAdapter bitbucketAdapter;

  @Autowired(required = false)
  private ICollaborationAdapter confluenceAdapter;

  private static final String ACTIVE = "active";

  @Value("${idmanager.group.opendevstack-users}")
  private String idmanagerUserGroup;

  @Value("${idmanager.group.opendevstack-administrators}")
  private String idmanagerAdminGroup;

  @Value("${openshift.project.upgrade}")
  private boolean ocUpgradeAllowed;

  @Qualifier("projectTemplateKeyNames")
  @Autowired
  private List<String> projectTemplateKeyNames;

  @Value("${provision.auth.provider}")
  private String authProvider;

  @Value("${adapters.confluence.enabled:true}")
  private boolean confluenceAdapterEnable;

  @Value("${frontend.spa.enabled:false}")
  private boolean spafrontendEnabled;

  @RequestMapping(value = ROUTE_ROOT_AND_ALL_RESOURCES_IN_PATH)
  public String redirect() {
    return FORWARD_TO_ROOT;
  }

  @RequestMapping(ROUTE_ROOT)
  public String rootRedirect() {

    if (isSpafrontendEnabled()) {
      return REDIRECT_TO_SPA_FRONTEND_INDEX_HTML;
    }

    if (!isAuthenticated()) {
      return redirectToLogin();
    }
    return REDIRECT_TO_HOME;
  }

  @RequestMapping(ROUTE_HOME)
  public String home(Model model) {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    if (!isAuthenticated()) {
      return redirectToLogin();
    }
    model.addAttribute("classActiveHome", ACTIVE);
    return "home";
  }

  private String redirectToNFEContext() {

    if (isAuthProviderOauth2()) {
      return REDIRECT_TO_SPA_FRONTEND_INDEX_HTML + "?sso=" + isAuthProviderOauth2();
    } else {
      return REDIRECT_TO_SPA_FRONTEND_INDEX_HTML;
    }
  }

  @RequestMapping(ROUTE_PROVISION)
  public String provisionProject(Model model) {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    if (!isAuthenticated()) {
      return redirectToLogin();
    } else {
      model.addAttribute("jiraProjects", storageAdapter.listProjectHistory());
      model.addAttribute("quickStarter", jenkinspipelineAdapter.getQuickstarterJobs());
      model.addAttribute("idmanagerUserGroup", idmanagerUserGroup.toLowerCase());
      model.addAttribute("idmanagerAdminGroup", idmanagerAdminGroup.toLowerCase());
      model.addAttribute("ocUpgradeAllowed", ocUpgradeAllowed);
      model.addAttribute("projectTypes", projectTemplateKeyNames);
      model.addAttribute(
          "specialPermissionSchemeEnabled", jiraAdapter.isSpecialPermissionSchemeEnabled());
    }
    model.addAttribute("classActiveNew", ACTIVE);
    return "provision";
  }

  @RequestMapping(ROUTE_LOGIN)
  public String login(Model model) {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    if (isAuthProviderOauth2()) {
      return "oauth2Login";
    }
    return "crowdLogin";
  }

  @RequestMapping(ROUTE_HISTORY)
  public String history(Model model) {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    if (!isAuthenticated()) {
      return redirectToLogin();
    }
    model.addAttribute("classActiveHistory", ACTIVE);
    model.addAttribute("projectHistory", storageAdapter.listProjectHistory());
    return "history";
  }

  @RequestMapping(ROUTE_ABOUT)
  public String about(Model model) {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    if (!isAuthenticated()) {
      return redirectToLogin();
    }

    model.addAttribute("classActiveAbout", ACTIVE);
    model.addAttribute("aboutChanges", storageAdapter.listAboutChangesData().getAboutDataList());

    // add endpoint map
    Map<String, String> endpoints = new HashMap<>();
    endpoints.put("JIRA", jiraAdapter.getAdapterApiUri());
    endpoints.put("GIT", bitbucketAdapter.getAdapterApiUri());

    if (confluenceAdapterEnable) {
      endpoints.put("CONFLUENCE", confluenceAdapter.getAdapterApiUri());
    } else {
      endpoints.put("CONFLUENCE", "no project created (confluence is disabled by configuration!)");
    }

    model.addAttribute("endpointMap", endpoints);

    model.addAttribute("idmanagerUserGroup", idmanagerUserGroup.toLowerCase());
    model.addAttribute("idmanagerAdminGroup", idmanagerAdminGroup.toLowerCase());
    model.addAttribute("email", manager.getUserEmail());
    model.addAttribute("username", manager.getUserName());
    return "about";
  }

  private String redirectToLogin() {
    return REDIRECT_TO_LOGIN;
  }

  public static Set<String> getUserRoles() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    Set<String> roles = new HashSet<>();

    if (null != authentication) {
      authentication.getAuthorities().forEach(e -> roles.add(e.getAuthority()));
    }
    return roles;
  }

  @RequestMapping(value = ROUTE_LOGOUT, method = RequestMethod.GET)
  public String logoutPage() {

    if (isSpafrontendEnabled()) {
      return redirectToNFEContext();
    }

    try {
      manager.invalidateIdentity();
    } catch (Exception eAllLogout) {
      logger.warn("Exception thrown on logout request!", eAllLogout);
    }
    return REDIRECT_TO_LOGIN_WITH_LOGOUT;
  }

  private boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (null != authentication) {
      if (isAuthProviderOauth2()) {
        return authentication.isAuthenticated();
      }

      return (authentication.isAuthenticated() && manager.getToken() != null);
    }

    return false;
  }

  @Autowired
  public void setCustomAuthenticationManager(IODSAuthnzAdapter manager) {
    this.manager = manager;
  }

  @Autowired
  public void setJobExecutionAdapter(IJobExecutionAdapter jenkinspipelineAdapter) {
    this.jenkinspipelineAdapter = jenkinspipelineAdapter;
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

  private boolean isAuthProviderOauth2() {
    return authProvider.equals(OAUTH_2_PROVIDER_KEY);
  }

  public boolean isSpafrontendEnabled() {
    return spafrontendEnabled;
  }

  public void setSpafrontendEnabled(boolean spafrontendEnabled) {
    this.spafrontendEnabled = spafrontendEnabled;
  }
}
