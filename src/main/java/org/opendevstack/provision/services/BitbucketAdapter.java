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
import org.opendevstack.provision.model.BitbucketData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.RepositoryData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.util.CrowdCookieJar;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RundeckJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
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

  @Autowired
  private RundeckJobStore jobStore;

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

  private static String PROJECT_PATTERN = "%s%s/projects";

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

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
          logger.debug("create repo for quickstarter: " + option.get("component_id") + " in " + project.key);

          String repoName = (String.format("%s-%s", project.key, option.get("component_id"))).toLowerCase().replace('_','-');
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
            if(result != null) {
              createWebHooksForRepository(result, project, option.get("component_id"), crowdCookieValue);
            }
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
            logger.error("Error in creating repo: " + option.get("component_id"), ex);
            throw new IOException(
            	"Error in creating repo: " + option.get("component_id") + "\n" +
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
        Map<String, List<Link>> links = result.getLinks();
        if (links != null) {
          repoLinks.put(result.getName(), result.getLinks());
        }
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
      String component, String crowdCookie) {

    for (String openShiftEnv : webHookEnvironments.split(",")) {
      /*
       * bitbucket.webhook.url=<api_host>/oapi/v1/namespaces/%s-cd/
       * buildconfigs/%s-%s/webhooks/secret101/generic bitbucket.webhook.environments=dev,test
       */
      String webHookUrl =
          String.format(baseWebHookUrlPattern, project.key.toLowerCase(), component, openShiftEnv);
      String webHookRshinyUrl = String.format(baseWebHookRshinyUrlPattern,
          project.key.toLowerCase(), openShiftEnv, component);

      logger.info("classic hook: " + webHookUrl + " -- " + webHookRshinyUrl);

      String url = String.format("%s/plugins/servlet/webhooks/repository/%s/%s/settings",
      bitbucketUri, project.key, repo.getSlug());

      String[] hooks = {webHookUrl, webHookRshinyUrl};

      int i = 0;
      for (String hook : hooks) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("edit", "");

        RequestBody body = new FormBody.Builder()
            .add("title", String.format("%s-%s-webhook-%d", repo.getSlug(), openShiftEnv, i))
            .add("enabled", "on").add("isRepoPush", "on").add("isPrMerged", "on")
            .addEncoded("url", hook).build();
        Request request = new Request.Builder().url(urlBuilder.build()).post(body).build();
        try {
          Response response = client.getClient(crowdCookie).newCall(request).execute();
          if (response.isSuccessful()) {
            logger.info("Successful webhook on cd project created: \n" + hook);
          }
          response.close();
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

    logger.debug("About to create bitbucket project for: " + bbProject.getKey());
    logger.debug(json);

    BitbucketData projectData =
        (BitbucketData) this.post(buildBasePath(), json, crowdCookieValue, BitbucketData.class);

    if (project.createpermissionset)
    {
	    setProjectPermissions(projectData, "groups", globalKeyuserRoleName, crowdCookieValue);    
	    setProjectPermissions(projectData, "groups", project.adminGroup, crowdCookieValue);
    }
	 
    setProjectPermissions(projectData, "groups", defaultUserGroup, crowdCookieValue);
    setProjectPermissions(projectData, "users", technicalUser, crowdCookieValue);

    if (project.admin != null) 
    {
    	setProjectPermissions(projectData, "users", project.admin, crowdCookieValue);
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

    setRepositoryPermissions(data, projectKey, "groups", repo.getUserGroup(), crowdCookieValue);
    setRepositoryPermissions(data, projectKey, "users", technicalUser, crowdCookieValue);

    return data;
  }

  protected void setProjectPermissions(BitbucketData data, String pathFragment, String groupOrUser,
      String crowdCookieValue) throws IOException {
    String basePath = buildBasePath();
    String url = String.format("%s/%s/permissions/%s", basePath, data.getKey(), pathFragment);
    // http://192.168.56.31:7990/rest/api/1.0/projects/{projectKey}/permissions/groups
    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
    // utschig - allow group to create new repos (rather than just read / write)
    urlBuilder.addQueryParameter("permission", "PROJECT_ADMIN");
    urlBuilder.addQueryParameter("name", groupOrUser);
    this.put(urlBuilder.build(), crowdCookieValue);
  }

  protected void setRepositoryPermissions(RepositoryData data, String key, String pathFragment,
      String groupOrUser, String crowdCookieValue) throws IOException {
    String basePath = buildBasePath();
    String url = String.format("%s/%s/repos/%s/permissions/groups", basePath, key, data.getSlug());

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

  private void put(HttpUrl url, String crowdCookieValue) throws IOException {

    Request req = new Request.Builder().url(bitbucketUri).get().build();
    Response resp = client.getClient(crowdCookieValue).newCall(req).execute();
    Headers hds = resp.headers();

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, "");
    Request request = new Request.Builder().url(url).put(body).build();

    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(respBody);
    resp.close();
    response.close();

  }

  public static BitbucketProject createBitbucketProject(ProjectData jiraProject) {
    BitbucketProject project = new BitbucketProject();
    project.setKey(jiraProject.key);
    project.setName(jiraProject.name);
    project.setDescription((jiraProject.description != null) ? jiraProject.description : "");
    return project;
  }

  protected Object post(String url, String json, String crowdCookieValue, Class clazz)
      throws IOException {
    client.getSessionId(bitbucketUri);

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Request request = new Request.Builder().url(url).post(body).build();

    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(response.code() + "> " +  respBody);

    if (response.code() == 401) {
    	throw new IOException("You are not authorized to create this resource (" +  url + "): " + respBody);
    }
    
    if (response.code() == 409) {
      throw new IOException("Resource creation failed, resource already exists");
    }

    response.close();
    return new ObjectMapper().readValue(respBody, clazz);
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
