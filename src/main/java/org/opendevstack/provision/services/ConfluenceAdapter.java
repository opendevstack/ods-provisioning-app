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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.SpaceData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.Context;
import org.opendevstack.provision.model.confluence.JiraServer;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.rest.core.Path;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service to interact with and add Spaces
 *
 * @author Brokmeier, Pascal
 */

@Service
public class ConfluenceAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ConfluenceAdapter.class);

  @Value("${confluence.api.path}")
  private String confluenceApiPath;

  @Value("${confluence.json.rpc.api.path}")
  private String confluenceLegacyApiPath;
  
  @Value("${confluence.uri}")
  private String confluenceUri;

  @Value("${jira.uri}")
  private String jiraUri;

  @Autowired
  RestClient client;

  @Value("${confluence.blueprint.key}")
  private String confluenceBlueprintKey;

  private static String SPACE_PATTERN = "%s%s/create-dialog/1.0/space-blueprint/create-space";
  private static String BLUEPRINT_PATTERN =
      "%s%s/create-dialog/1.0/space-blueprint/dialog/web-items";
  private static String JIRA_SERVER = "%s%s/jiraanywhere/1.0/servers";

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  // Pattern to use for project with id
  private static String URL_PATTERN = "%s/api/%s";

  private String crowdCookieValue = null;

  @Value("${confluence.permission.filepattern}")
  private String confluencePermissionFilePattern;
  
  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;
  
  public ProjectData createConfluenceSpaceForProject(ProjectData project, String crowdCookieValue)
      throws IOException {
    this.crowdCookieValue = crowdCookieValue;
    SpaceData space = callCreateSpaceApi(createSpaceData(project), crowdCookieValue);
    project.confluenceUrl = space.getUrl();
    
    if (project.createpermissionset) 
    {
    	try 
    	{
    		updateSpacePermissions(project, crowdCookieValue);
    	} catch (Exception createPermissions) 
    	{
    		// continue - we are ok if permissions fail, because the admin has access, and create the set
    		logger.error("Could not create project: " + project.key + "\n Exception: " 
    				+ createPermissions.getMessage());
    	}
    }
    
    return project;
  }

  protected SpaceData callCreateSpaceApi(Space space, String crowdCookieValue) throws IOException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(space);

    logger.debug(json);

    String path = String.format(SPACE_PATTERN, confluenceUri, confluenceApiPath);
    return this.post(path, json, crowdCookieValue, SpaceData.class);
  }

  protected Space createSpaceData(ProjectData project) throws IOException {
    String confluenceBlueprintId = getBluePrintId();
    String jiraServerId = getJiraServerId();

    Space space = new Space();
    space.setSpaceBlueprintId(confluenceBlueprintId);
    space.setName(project.name);
    space.setSpaceKey(project.key);
    space.setDescription(project.description);

    Context context = new Context();
    context.setName(project.name);
    context.setSpaceKey(project.key);
    context.setAtlToken("undefined");
    context.setNoPageTitlePrefix("true");
    context.setJiraServer(jiraServerId);
    context.setJiraServerId(jiraServerId);
    context.setJiraProject(project.jiraId);
    context.setProjectKey(project.key);
    context.setProjectName(project.name);
    context.setDescription(project.description);
    space.setContext(context);
    return space;
  }

  private String getJiraServerId() throws IOException {
    String jiraServerId = null;
    String url = String.format(JIRA_SERVER, confluenceUri, confluenceApiPath);
    List<Object> server = getList(url, crowdCookieValue, new TypeReference<List<JiraServer>>() {});
    for (Object obj : server) {
      logger.debug(obj.toString());
      JiraServer jiraServer = (JiraServer) obj;
      if (jiraServer.getUrl().equals(jiraUri)) {
        jiraServerId = jiraServer.getId();
      }
    }
    return jiraServerId;
  }

  private String getBluePrintId() throws IOException {
    String bluePrintId = null;
    String url = String.format(BLUEPRINT_PATTERN, confluenceUri, confluenceApiPath);
    List<Object> blueprints =
        getList(url, crowdCookieValue, new TypeReference<List<Blueprint>>() {});
    for (Object obj : blueprints) {
      Blueprint blueprint = (Blueprint) obj;
      if (blueprint.getBlueprintModuleCompleteKey().equals(confluenceBlueprintKey)) {
        bluePrintId = blueprint.getContentBlueprintId();
      }
    }
    return bluePrintId;
  }


  private List<Object> getList(String url, String crowdCookieValue, TypeReference reference)
      throws IOException {

    client.getSessionId(confluenceUri);

    Request request =
        new Request.Builder().url(url).addHeader("Accept", "application/json").get().build();

    logger.debug("calling url : " + url);
    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(respBody);
    if (!response.isSuccessful()) {
    	throw new IOException("Could not retrieve blueprints: " + respBody);
    }

    return new ObjectMapper().readValue(respBody, reference);
  }

  protected <T> T post(String url, String json, String crowdCookieValue, Class valueType) throws IOException {

    client.getSessionId(confluenceUri);

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Request request = new Request.Builder().url(url).post(body).build();

    logger.debug("Call to: " + url);
    
    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(response.code() + " > " + respBody);
    response.close();
    
    if (response.code() != 200) 
    {
      throw new IOException("Could not create " + valueType.getName() + " : " + respBody);
    }
        
    return (T) new ObjectMapper().readValue(respBody, valueType);
  }

  protected void updateSpacePermissions (ProjectData data, String crowdCookieValue) throws IOException 
  {
      PathMatchingResourcePatternResolver pmrl = new PathMatchingResourcePatternResolver(
    	 Thread.currentThread().getContextClassLoader());
      
      Resource [] permissionFiles = pmrl.getResources(confluencePermissionFilePattern);
      
      logger.debug("Found permissionsets: "+ permissionFiles.length);
      
      for (int i = 0; i < permissionFiles.length; i++)
      {
    	  String permissionFilename = permissionFiles[i].getFilename();
    	  
    	  BufferedReader reader = new BufferedReader (new InputStreamReader(permissionFiles[i].getInputStream()));
    	  
    	  String permissionset = new String(reader.readLine());
    	  
    	  reader.close();
    	  
    	  permissionset = permissionset.replace("SPACE_NAME", data.key);
    	  
    	  if (permissionFilename.contains("adminGroup")) {
    		  permissionset = permissionset.replace("SPACE_GROUP", data.adminGroup);
    	  } else if (permissionFilename.contains("userGroup")) {
    		  permissionset = permissionset.replace("SPACE_GROUP", data.userGroup);
      	  } else if (permissionFilename.contains("readonlyGroup")) {
    		  permissionset = permissionset.replace("SPACE_GROUP", data.readonlyGroup);  
      	  } else if (permissionFilename.contains("keyuserGroup")) {
    		  permissionset = permissionset.replace("SPACE_GROUP", globalKeyuserRoleName);  
      	  } else if (permissionFilename.contains("admin")) {
    		  permissionset = permissionset.replace("SPACE_USER", data.admin);  
      	  } else {
      		  
      	  }
    	  
    	  String path = String.format("%s%s/addPermissionsToSpace", confluenceUri, confluenceLegacyApiPath);
    	  
    	  post(path, permissionset, crowdCookieValue, String.class); 
      }
  }
  
}
