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
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

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

  private static final String SPACE_PATTERN = "%s%s/create-dialog/1.0/space-blueprint/create-space";
  private static final String BLUEPRINT_PATTERN =
      "%s%s/create-dialog/1.0/space-blueprint/dialog/web-items";
  private static final String JIRA_SERVER = "%s%s/jiraanywhere/1.0/servers";
  
  private static final String SPACE_GROUP = "SPACE_GROUP";

  private String crowdCookieValue = null;

  @Value("${confluence.permission.filepattern}")
  private String confluencePermissionFilePattern;
  
  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;
  
  @Autowired
  Environment environment;
  
  @Autowired
  List<String> projectTemplateKeyNames;
  
  @Value("${project.template.default.key}")
  private String defaultProjectKey;
  
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
      String path = String.format(SPACE_PATTERN, confluenceUri, confluenceApiPath);
	  return client.callHttp(path, space, crowdCookieValue, false, RestClient.HTTP_VERB.POST, SpaceData.class);
  }

  Space createSpaceData(ProjectData project) throws IOException {
    String confluenceBlueprintId = getBluePrintId(project.projectType);
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
      logger.debug("Server: {}", obj);
      JiraServer jiraServer = (JiraServer) obj;
      if (jiraServer.getUrl().equals(jiraUri)) {
        jiraServerId = jiraServer.getId();
      }
    }
    return jiraServerId;
  }

  private String getBluePrintId(String projectTypeKey) throws IOException
  {
    String bluePrintId = null;
    String url = String.format(BLUEPRINT_PATTERN, confluenceUri, confluenceApiPath);
    List<Object> blueprints =
        getList(url, crowdCookieValue, new TypeReference<List<Blueprint>>() {});
    
    String template =
    	calculateConfluenceSpaceTypeAndTemplateFromProjectType(projectTypeKey);
    
    for (Object obj : blueprints) {
      Blueprint blueprint = (Blueprint) obj;
      logger.debug("Blueprint: {} searchKey: {}", blueprint.getBlueprintModuleCompleteKey(), template);
      if (blueprint.getBlueprintModuleCompleteKey().equals(template)) {
        bluePrintId = blueprint.getContentBlueprintId();
        break;
      }
    }
    if (bluePrintId == null) 
    {
    	// default
    	return getBluePrintId(null);
    }
    return bluePrintId;
  }


  List<Object> getList(String url, String crowdCookieValue, TypeReference reference)
      throws IOException {
    client.getSessionId(confluenceUri);
    
    return (List<Object>) client.callHttpTypeRef(url, null, crowdCookieValue, false, RestClient.HTTP_VERB.GET, reference);
  }

  int updateSpacePermissions (ProjectData data, String crowdCookieValue) throws IOException 
  {
      PathMatchingResourcePatternResolver pmrl = new PathMatchingResourcePatternResolver(
    	 Thread.currentThread().getContextClassLoader());
      
      Resource [] permissionFiles = pmrl.getResources(confluencePermissionFilePattern);
      
      int updatedPermissions = 0;

      logger.debug("Found permission sets: {}", permissionFiles.length);

      for (Resource permissionFile : permissionFiles) {
          String permissionFilename = permissionFile.getFilename();

          try (BufferedReader reader = new BufferedReader(new InputStreamReader(permissionFile.getInputStream()))) {
              String permissionset;

              // we know it's a singular pseudo json line
              permissionset = reader.readLine();
              permissionset = permissionset.replace("SPACE_NAME", data.key);

              if (permissionFilename.contains("adminGroup")) {
                  permissionset = permissionset.replace(SPACE_GROUP, data.adminGroup);
              } else if (permissionFilename.contains("userGroup")) {
                  permissionset = permissionset.replace(SPACE_GROUP, data.userGroup);
              } else if (permissionFilename.contains("readonlyGroup")) {
                  permissionset = permissionset.replace(SPACE_GROUP, data.readonlyGroup);
              } else if (permissionFilename.contains("keyuserGroup")) {
                  permissionset = permissionset.replace(SPACE_GROUP, globalKeyuserRoleName);
              }

              String path = String.format("%s%s/addPermissionsToSpace", confluenceUri, confluenceLegacyApiPath);

              client.callHttp(path, permissionset, crowdCookieValue, false, RestClient.HTTP_VERB.POST, String.class);

              updatedPermissions++;
          }
      }
      return updatedPermissions;
  }
  
  public String getConfluenceAPIPath () {
	  return confluenceUri + confluenceApiPath;
  }

  public String calculateConfluenceSpaceTypeAndTemplateFromProjectType
	(String projectTypeKey) 
  {
	    String confluencetemplateKeyPrefix = "confluence.blueprint.key.";

	    String template =
	    	(projectTypeKey != null && !projectTypeKey.equals(defaultProjectKey) && 
	    		environment.containsProperty(confluencetemplateKeyPrefix + projectTypeKey) && 
	    		projectTemplateKeyNames.contains(projectTypeKey)) ?
	    		environment.getProperty(confluencetemplateKeyPrefix + projectTypeKey) : 
	    			confluenceBlueprintKey;
	    		
	    return template;
  }
  
}
