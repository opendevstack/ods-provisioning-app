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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.BitbucketData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.RepositoryData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.Webhook;
import org.opendevstack.provision.util.CrowdCookieJar;
import org.opendevstack.provision.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service to interact with Bitbucket and to create projects and repositories
 *
 * @author Brokmeier, Pascal
 */

@Service
public class BitbucketAdapter {

  private static final Logger logger = LoggerFactory.getLogger(BitbucketAdapter.class);

  CrowdCookieJar cookieJar = new CrowdCookieJar();

  @Autowired
  RundeckAdapter rundeckAdapter;

  @Value("${bitbucket.api.path}")
  private String bitbucketApiPath;

  @Value("${bitbucket.uri}")
  private String bitbucketUri;

  @Value("${atlassian.domain}")
  private String confluenceDomain;

  @Value("${bitbucket.webhook.url}")
  private String baseWebHookUrlPattern;

  @Value("${bitbucket.webhook.rshiny.url}")
  private String baseWebHookRshinyUrlPattern;


  @Value("${bitbucket.webhook.environments}")
  private String webHookEnvironments;

  @Value("${bitbucket.default.user.group}")
  private String defaultUserGroup;

  @Value("${bitbucket.technical.user}")
  private String technicalUser;

  
  @Value("${global.keyuser.role.name}") 
  private String globalKeyuserRoleName;
  
  @Autowired
  RestClient client;

  @Autowired
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  CustomAuthenticationManager manager;
    
  private static String PROJECT_PATTERN = "%s%s/projects";

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  private static final String COMPONENT_ID_KEY = "component_id";
  
  private static final String ID_GROUPS = "groups";
  private static final String ID_USERS = "users";
  
  public enum PROJECT_PERMISSIONS {
	  PROJECT_ADMIN,
	  PROJECT_WRITE,
	  PROJECT_READ
  }
  
  public ProjectData createBitbucketProjectsForProject(ProjectData project, String crowdCookieValue)
      throws IOException {
    BitbucketData data = callCreateProjectApi(project, crowdCookieValue);
    // data.setUrl(String.format("%s/projects/%s", bitbucketUri, data.getKey()));

    project.bitbucketUrl = data.getLinks().get("self").get(0).getHref();
    return project;
  }

  public ProjectData createRepositoriesForProject(ProjectData project, String crowdCookieValue) throws IOException {
	  
	  CrowdUserDetails userDetails =
        (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

	  logger.debug("Creating quickstartProjects");
	  
      List<RepositoryData> repos = new ArrayList<>();
      Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
      List<Map<String, String>> newOptions = new ArrayList<>();
      if(project.quickstart != null) {
    	  
    	  logger.debug("new quickstarters: " + project.quickstart.size());
    	  
    	  for(Map<String, String> option : project.quickstart) {
          logger.debug("create repo for quickstarter: " + option.get(COMPONENT_ID_KEY) + " in " + project.key);

          String repoName = (String.format("%s-%s", project.key, option.get(COMPONENT_ID_KEY))).toLowerCase().replace('_','-');
          Repository repo = new Repository();
          repo.setName(repoName);
          
          if (project.createpermissionset)
          {
        	  repo.setAdminGroup(project.adminGroup);
        	  repo.setUserGroup(project.userGroup);
          } else {
        	  repo.setAdminGroup(this.defaultUserGroup);
        	  repo.setUserGroup(this.defaultUserGroup);
          }
          
          try {
            RepositoryData result = callCreateRepoApi(project.key, repo, crowdCookieValue);
            createWebHooksForRepository(result, project, option.get(COMPONENT_ID_KEY), crowdCookieValue, option.get("component_type"));
            Map<String, List<Link>> links = result.getLinks();
            if(links != null) {
              repoLinks.put(result.getName(), result.getLinks());
              for(Link repoLink : links.get("clone")) {
                String href = repoLink.getHref();
                if(repoLink.getName().equalsIgnoreCase("http")) {
                  href = href.replace(URLEncoder.encode(userDetails.getUsername(), "UTF-8"), technicalUser);
                }
                option.put(String.format("git_url_%s", repoLink.getName()), href);
              }
              newOptions.add(option);
            }
          } catch (IOException ex)
          {
            logger.error("Error in creating repo: " + option.get(COMPONENT_ID_KEY), ex);
            throw new IOException(
            	"Error in creating repo: " + option.get(COMPONENT_ID_KEY) + "\n" +
            			"details: " + ex.getMessage());
          }
        }
        project.quickstart = newOptions;
      }

    if (project.repositories != null) {
      project.repositories.putAll(repoLinks);
    } else {
      project.repositories = repoLinks;
    }
    return project;
  }

  public ProjectData createAuxiliaryRepositoriesForProject(ProjectData project,
      String crowdCookieValue, String[] auxiliaryRepos) {
    Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
    for (String name : auxiliaryRepos) {
      Repository repo = new Repository();
      String repoName = String.format("%s-%s", project.key.toLowerCase(), name);
      repo.setName(repoName);

      if (project.createpermissionset)
      {
    	  repo.setAdminGroup(project.adminGroup);
    	  repo.setUserGroup(project.userGroup);
      } else {
    	  repo.setAdminGroup(this.defaultUserGroup);
    	  repo.setUserGroup(this.defaultUserGroup);
      }

      try {
        RepositoryData result = callCreateRepoApi(project.key, repo, crowdCookieValue);
        repoLinks.put(result.getName(), result.getLinks());
      } catch (IOException ex) {
        logger.error("Error in creating auxiliary repo", ex);
      }
    }
    if (project.repositories != null) {
      project.repositories.putAll(repoLinks);
    } else {
      project.repositories = repoLinks;
    }
    return project;
  }

  protected void createWebHooksForRepository(RepositoryData repo, ProjectData project,
      String component, String crowdCookie, String componentType) {

    for (String openShiftEnv : webHookEnvironments.split(",")) {
      /*
       * bitbucket.webhook.url=<api_host>/oapi/v1/namespaces/%s-cd/
       * buildconfigs/%s-%s/webhooks/secret101/generic bitbucket.webhook.environments=dev,test
       */
      String webHookUrlCI = 
        	String.format(baseWebHookUrlPattern, project.key.toLowerCase(), component, openShiftEnv);
      String webHookUrlDirect =
      		String.format(baseWebHookRshinyUrlPattern, project.key.toLowerCase(), openShiftEnv, component);
      
      logger.info("created hook: " + webHookUrlCI + " -- " + webHookUrlDirect);

      String[] hooks = {webHookUrlCI, webHookUrlDirect};
      
      // projects/CLE200/repos/cle200-be-node-express/webhooks
      String url = String.format("%s/%s/repos/%s/webhooks",
      buildBasePath(), project.key, repo.getSlug());

      int i = 0;
      for (String hook : hooks)
      {
    	Webhook wHook = new Webhook();
    		wHook.setName(String.format("%s-%s-webhook-%d", repo.getSlug(), openShiftEnv, i));	
    		wHook.setActive(true);
    		wHook.setUrl(hook);
    		List<String> events = new ArrayList<String>();
    		events.add("repo:refs_changed");
    		events.add("pr:merged");
    		wHook.setEvents(events);
    	  
        try {
        	ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        	String json = ow.writeValueAsString(wHook);
        	this.post(url, json, crowdCookie, Webhook.class);
        } catch (IOException ex) {
          logger.error("Error in webhook call", ex);
        }
        i++;
      }
    }
  }

  protected BitbucketData callCreateProjectApi(ProjectData project, String crowdCookieValue)
      throws IOException {
	  
	BitbucketProject bbProject =  createBitbucketProject(project);
	  
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(bbProject);

    logger.debug("About to create bitbucket project for: " + bbProject.getKey() + "\n" + json);

    BitbucketData projectData =
        (BitbucketData) this.post(buildBasePath(), json, crowdCookieValue, BitbucketData.class);

    if (project.createpermissionset)
    {
	    setProjectPermissions(projectData, ID_GROUPS, globalKeyuserRoleName, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_ADMIN);    
	    setProjectPermissions(projectData, ID_GROUPS, project.adminGroup, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_ADMIN);
	    setProjectPermissions(projectData, ID_GROUPS, project.userGroup, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_WRITE);
	    setProjectPermissions(projectData, ID_GROUPS, project.readonlyGroup, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_READ);
    }
	
    // set those in any case
    setProjectPermissions(projectData, ID_GROUPS, defaultUserGroup, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_WRITE);
    setProjectPermissions(projectData, ID_USERS, technicalUser, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_WRITE);

    if (project.admin != null) 
    {
    	setProjectPermissions(projectData, ID_USERS, project.admin, crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_ADMIN);
    }
    
    return projectData;
  }

  protected RepositoryData callCreateRepoApi(String projectKey, Repository repo,
      String crowdCookieValue) throws IOException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(repo);
    String path = String.format("%s/%s/repos", buildBasePath(), projectKey);

    RepositoryData data =
        (RepositoryData) this.post(path, json, crowdCookieValue, RepositoryData.class);

    setRepositoryPermissions(data, projectKey, ID_GROUPS, repo.getUserGroup(), crowdCookieValue);
    setRepositoryPermissions(data, projectKey, ID_USERS, technicalUser, crowdCookieValue);

    return data;
  }

  protected void setProjectPermissions(BitbucketData data, String pathFragment, String groupOrUser,
      String crowdCookieValue, PROJECT_PERMISSIONS rights) throws IOException {
    String basePath = buildBasePath();
    String url = String.format("%s/%s/permissions/%s", basePath, data.getKey(), pathFragment);
    // http://192.168.56.31:7990/rest/api/1.0/projects/{projectKey}/permissions/groups
    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
    // utschig - allow group to create new repos (rather than just read / write)
    urlBuilder.addQueryParameter("permission", rights.toString());
    urlBuilder.addQueryParameter("name", groupOrUser);
    this.put(urlBuilder.build(), crowdCookieValue);
  }

  protected void setRepositoryPermissions(RepositoryData data, String key, String userOrGroup,
      String groupOrUser, String crowdCookieValue) throws IOException {
    String basePath = buildBasePath();
    String url = String.format("%s/%s/repos/%s/permissions/%s", basePath, key, data.getSlug(), userOrGroup);

    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
    // allow people to modify settings (webhooks)
    urlBuilder.addQueryParameter("permission", "REPO_ADMIN");
    urlBuilder.addQueryParameter("name", groupOrUser);

    this.put(urlBuilder.build(), crowdCookieValue);
  }

  protected String buildBasePath() {
    String basePath = String.format(PROJECT_PATTERN, bitbucketUri, bitbucketApiPath);
    return basePath;
  }

  protected Object post(String url, String json, String crowdCookieValue, Class clazz)
	      throws IOException {
    client.getSessionId(bitbucketUri);

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Builder builder = new Request.Builder().url(url).post(body);

	String credentials =
			Credentials.basic(this.crowdUserDetailsService.loadUserByToken(crowdCookieValue).getUsername(),
					manager.getUserPassword());
	builder = builder.addHeader("Authorization", credentials);
	Response response = client.getClientFresh(crowdCookieValue).newCall(builder.build()).execute();

    String respBody = response.body().string();

    logger.debug(url, " > " + json + "\n" + response.code() + "> " +  respBody);

    if (response.code() == 401) {
    	throw new IOException("You are not authorized to create this resource (" +  url + "): " + respBody);
    }
    
    if (response.code() == 409) {
      throw new IOException("Resource creation failed, resource already exists");
    }

    response.close();
    return new ObjectMapper().readValue(respBody, clazz);
  }
  
  
  private void put(HttpUrl url, String crowdCookieValue) throws IOException {

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, "");
    Builder builder = new Request.Builder().url(url).put(body);

	String credentials =
			Credentials.basic(this.crowdUserDetailsService.loadUserByToken(crowdCookieValue).getUsername(),
					manager.getUserPassword());
	builder = builder.addHeader("Authorization", credentials);
    
	Response response = client.getClientFresh(crowdCookieValue).newCall(builder.build()).execute();
    String respBody = response.body().string();

    logger.debug(response.code() + ">" + respBody);
    response.close();

  }

  public static BitbucketProject createBitbucketProject(ProjectData jiraProject) {
    BitbucketProject project = new BitbucketProject();
    project.setKey(jiraProject.key);
    project.setName(jiraProject.name);
    project.setDescription((jiraProject.description != null) ? jiraProject.description : "");
    return project;
  }


  /**
   * Get the bitbucket http endpoint
   * 
   * @return the endpoint - cant be null
   */
  public String getEndpointUri() {
    return bitbucketUri;
  }

}
