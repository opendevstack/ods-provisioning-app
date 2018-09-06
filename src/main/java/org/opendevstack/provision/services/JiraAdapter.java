package org.opendevstack.provision.services;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.Permission;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * created by:
 * OPITZ CONSULTING Deutschland GmbH
 *
 * @author Brokmeier, Pascal <p> To communicate with Jira, we use the Jira REST API
 *         https://developer.atlassian.com/jiradev/jira-apis/jira-rest-apis To ease the burden of
 *         working with a REST API, there is a jira client https://ecosystem.atlassian.net/wiki/display/JRJC/
 */
@Service
public class JiraAdapter {

  private static final Logger logger = LoggerFactory.getLogger(JiraAdapter.class);

  @Value("${jira.api.path}")
  private String jiraApiPath;

  @Value("${jira.uri}")
  private String jiraUri;

  @Value("${jira.permission.filepattern}")
  private String jiraPermissionFilePattern;
  
  //Pattern to use for project with id
  private static String URL_PATTERN = "%s%s/project/%s";

  private static final MediaType JSON_MEDIA_TYPE = MediaType
      .parse("application/json; charset=utf-8");

  @Autowired
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  CustomAuthenticationManager manager;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;
    
  @Autowired
  RestClient client;

  /**
   * Based on the information in the project object we create a jira project
   *
   * @param project data from the frontend
   * @param crowdCookieValue the value for the crowd cookie
   */
  public ProjectData createJiraProjectForProject(ProjectData project,
      String crowdCookieValue) throws Exception {
    try {
      client.getSessionId(jiraUri);

      logger.debug("Creating new jira project");

      Preconditions.checkNotNull(project.key);
      Preconditions.checkNotNull(project.name);
      
      CrowdUserDetails details = crowdUserDetailsService.loadUserByToken(crowdCookieValue);
      project.admins.add(new BasicUser(null, details.getUsername(), details.getFullName()));
      
      if (project.admin != null) 
      {
    	  //first one will be the lead!
    	  project.admins.clear();
          project.admins.add(new BasicUser(null, project.admin, project.admin));
      }
      
      FullJiraProject created = this
          .callJiraCreateProjectApi(this.buildJiraProjectPojoFromApiProject(project),
              crowdCookieValue);
      logger.debug("Created project: {}", created);
      created.getKey();
      project.jiraUrl = created.getSelf().toASCIIString();
      project.jiraId = created.id;
      
      if (project.createpermissionset) {
          createPermissions(project,crowdCookieValue);
      }
      
      return project;
    } catch (IOException e) {
      logger.error("Error in project creation", e);
      throw e;
    }

  }

  public FullJiraProject getProject(String id, String crowdCookieValue) {
    String url = String.format(URL_PATTERN, jiraUri, jiraApiPath, id);
    Request request = new Request
        .Builder()
        .url(url)
        .get()
        .build();
    try {
      String response = client.getClient(crowdCookieValue).newCall(request).execute().body().toString();
      logger.debug(response);
      return new ObjectMapper().readValue(response, FullJiraProject.class);

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  protected FullJiraProject callJiraCreateProjectApi(
      FullJiraProject jiraProject, String crowdCookieValue)
      throws IOException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(jiraProject);
    String path = String.format("%s%s/project", jiraUri, jiraApiPath);
    FullJiraProject created = this.post(path, json, crowdCookieValue, FullJiraProject.class, false);
    FullJiraProject returnProject = new FullJiraProject(created.getSelf(), created.getKey(),
        jiraProject.getName(), jiraProject.getDescription(), null, null, null, null, null, null,
        null, null);
    returnProject.id = created.id;
    return returnProject;
  }

  protected void createPermissions (ProjectData project, String crowdCookieValue) 
		  throws Exception 
  {
      PathMatchingResourcePatternResolver pmrl = new PathMatchingResourcePatternResolver(
    	 Thread.currentThread().getContextClassLoader());
      
      Resource [] permissionFiles = pmrl.getResources(jiraPermissionFilePattern);
      
      logger.debug("Found permissionsets: "+ permissionFiles.length);
      
      for (int i = 0; i < permissionFiles.length; i++)
      {
	      PermissionScheme singleScheme = 
	         new ObjectMapper().readValue(
	    		permissionFiles[i].getInputStream(), PermissionScheme.class);
	      
	      String permissionSchemeName = project.key + " PERMISSION SCHEME";
	      
	      singleScheme.setName(permissionSchemeName);
	      
	      // replace group with real group
	      for (Permission permission : singleScheme.getPermissions()) 
	      {
	    	  String group = permission.getHolder().getParameter();
	    	  
	    	  if ("adminGroup".equals(group)) {
	    		  permission.getHolder().setParameter(project.adminGroup);
	    	  } else if ("userGroup".equals(group)) {
	    		  permission.getHolder().setParameter(project.userGroup);
	    	  } else if ("readonlyGroup".equals(group)) {
	    		  permission.getHolder().setParameter(project.readonlyGroup);
	    	  } else if ("keyuserGroup".equals(group)) {
	    		  permission.getHolder().setParameter(globalKeyuserRoleName);
	    	  } else {
	    		  	    		  
	    	  }
	      }
	      logger.debug("Update permissionScheme " + permissionSchemeName +
	    	" location: " + permissionFiles[i].getFilename());
	      
	      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
	      
	      String json = ow.writeValueAsString(singleScheme);
	      String path = String.format("%s%s/permissionscheme", jiraUri, jiraApiPath);
	      singleScheme = post(path, json, crowdCookieValue, PermissionScheme.class, true);
	      
	      // update jira project
	      path = String.format("%s%s/project/%s/permissionscheme", jiraUri, jiraApiPath, project.key);
	      json = String.format("{ \"id\" : %s }", singleScheme.getId()); 
	      put(path, json, crowdCookieValue, FullJiraProject.class);	      
      }
  }
  
  protected <T> T post(String url, String json, String crowdCookieValue, Class valueType, boolean newClient)
      throws IOException {
	  
    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);

    Request request = new Request.Builder()
        .url(url)
        .post(body).addHeader("X-Atlassian-Token", "no-check")
        .build();

    logger.debug("Call to: " + url);
    
    Response response = null;
    if (newClient)
    {
    	String credentials =
    			Credentials.basic(this.crowdUserDetailsService.loadUserByToken(crowdCookieValue).getUsername(),
    					manager.getUserPassword());
    	request = new Request.Builder()
    	        .url(url)
    	        .post(body).addHeader("X-Atlassian-Token", "no-check")
    	        .addHeader("Authorization", credentials)
    	        .build();
    	response = client.getClientFresh(crowdCookieValue).newCall(request).execute();
    }
    else 
    {
    	response = client.getClient(crowdCookieValue).newCall(request).execute();	
    }
    	    
    String respBody = response.body().string();
    logger.debug(respBody);
    response.close();
    
    if (response.code() < 200 || response.code() >= 300)
    {
      throw new IOException(response.code()+": Could not create " +valueType.getName() + ": " + respBody);
    }
    
    return (T)new ObjectMapper().readValue(respBody, valueType);
  }

  protected <T> T  put(String url, String json, String crowdCookieValue, Class valueType)
      throws IOException {

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Request request = new Request.Builder()
        .url(url)
        .put(body).addHeader("X-Atlassian-Token", "no-check")
        .build();

    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(respBody);
    response.close();
    
    if (response.code() < 200 || response.code() >= 300)
    {
      throw new IOException(response.code()+": Could not update " +valueType.getName() + ": " + respBody);
    }
    
    return (T) new ObjectMapper().readValue(respBody, valueType);
  }
  
  protected FullJiraProject buildJiraProjectPojoFromApiProject(ProjectData s) {
    BasicUser lead = s.admins.get(0);
    String templateKey = "com.pyxis.greenhopper.jira:gh-scrum-template";
    String type = "software";

    return new FullJiraProject(null, s.key.toUpperCase(), s.name, s.description, lead, null, null, null, null, null,
        templateKey, type);
  }

  public String buildProjectKey(String name) {
    //build key
    String key = name;
    key = key.toUpperCase();
    key = key.replaceAll("\\s+", "");
    key = key.replaceAll("-", "_");
    key = key.length() > 5 ? (key.substring(0, 3) + key.substring(key.length()-2, key.length())) : key;
    return key;
  }

  public boolean keyExists(String key, String crowdCookieValue) {
    getSessionId();
/*    List<FullJiraProject> projects = getProjects(crowdCookieValue, key);
    for(FullJiraProject project : projects) {
      String normalizedProject = project.getKey().trim();
      String normalizedParam = key.trim();
      if(normalizedParam.equalsIgnoreCase(normalizedProject)) {
        return true;
      }
    } */ 
    return false; 
  }

  // refactor - to only look for the project by key that is to be created!
  public List<FullJiraProject> getProjects(String crowdCookieValue, String filter) 
  {
    getSessionId();
    String url = 
    	filter == null ? String.format("%s%s/project", jiraUri, jiraApiPath) :
        String.format("%s%s/project/%s", jiraUri, jiraApiPath, filter);
    	
    Request request = new Request
        .Builder()
        .url(url)
        .get()
        .build();
    try {
      Response response = client.getClient(crowdCookieValue).newCall(request).execute();
      String respBody = response.body().string();

      logger.debug(respBody);
      response.close();
      
      if (response.code() == 404) 
      {
          return new ArrayList<FullJiraProject>();
      }
      
      // then max one .. and the conversion will fail
      if (filter != null) 
      {
    	  List <FullJiraProject> results = new ArrayList<FullJiraProject>();
    	  results.add(new ObjectMapper().readValue(respBody, FullJiraProject.class));
    	  return results;
      }
      
      return new ObjectMapper().readValue(respBody, new TypeReference<List<FullJiraProject>>() {});
    } catch (JsonMappingException e) {
      // if for some odd reason serialization fails ... 
      List<FullJiraProject> returnList = new ArrayList<>();
      returnList.add(new FullJiraProject
    		  (null, filter,filter,filter,null, null,null,null, null,null,null, null));
    				  
      return returnList;
    } catch (IOException e) {
      logger.error("Error in getting projects", e);
      // if for nothing else - 
      return new ArrayList<FullJiraProject>();
    }
  }

  private void getSessionId() {
    try {
      client.getSessionId(jiraUri);
    } catch(IOException ex) {
      logger.error("Can not get session id", ex);
    }
  }

  public String getEndpointUri () {
  	return jiraUri;
  }
  
  
}
