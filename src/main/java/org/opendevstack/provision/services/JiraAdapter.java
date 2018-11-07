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
import org.opendevstack.provision.model.jira.Shortcut;
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

  @Value("${jira.project.template.key}")
  String jiraTemplateKey;
  
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

  public static enum HTTP_VERB {
	  PUT, 
	  POST,
	  GET
  }
  
  /**
   * Based on the information in the project object we create a jira project
   *
   * @param project data from the frontend
   * @param crowdCookieValue the value for the crowd cookie
   */
  public ProjectData createJiraProjectForProject(ProjectData project,
      String crowdCookieValue) throws IOException {
    try {
      client.getSessionId(jiraUri);

      logger.debug("Creating new jira project");

      Preconditions.checkNotNull(project.key);
      Preconditions.checkNotNull(project.name);
      
      CrowdUserDetails details = crowdUserDetailsService.loadUserByToken(crowdCookieValue);
      project.admins.add(new BasicUser(null, details.getUsername(), details.getFullName()));
      
      if (project.createpermissionset && project.admin != null && project.admin.length() > 0) 
      {
    	  //first one will be the lead!
    	  project.admins.clear();
          project.admins.add(new BasicUser(null, project.admin, project.admin));
      }
      
      FullJiraProject created = this
          .callJiraCreateProjectApi(this.buildJiraProjectPojoFromApiProject(project),
              crowdCookieValue);
      logger.debug("Created project: {}", created);
      project.jiraUrl = String.format ("%s/browse/%s", jiraUri, created.getKey());
      project.jiraId = created.id;
      
      if (project.createpermissionset) {
        createPermissions(project,crowdCookieValue);
      }
      
      return project;
    } catch (IOException eCreationException) {
      logger.error("Error in project creation", eCreationException);
      throw eCreationException;
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

    } catch (IOException eGetProjects) 
    {
      logger.error("Error getting projects: {}", eGetProjects);
      return null;
    }
  }

  protected FullJiraProject callJiraCreateProjectApi(
      FullJiraProject jiraProject, String crowdCookieValue)
      throws IOException 
  {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(jiraProject);
    String path = String.format("%s%s/project", jiraUri, jiraApiPath);
    FullJiraProject created = this.callHttp(path, json, crowdCookieValue, FullJiraProject.class, false, HTTP_VERB.POST);
    FullJiraProject returnProject = new FullJiraProject(created.getSelf(), created.getKey(),
        jiraProject.getName(), jiraProject.getDescription(), null, null, null, null, null, null,
        null, null);
    returnProject.id = created.id;
    return returnProject;
  }

  /**
   * Create permission set for jira project
   * @param project the project
   * @param crowdCookieValue the crowd cookie
   * @return the number of created permission sets
   */
  protected int createPermissions (ProjectData project, String crowdCookieValue) 
  {
      PathMatchingResourcePatternResolver pmrl = new PathMatchingResourcePatternResolver(
    	 Thread.currentThread().getContextClassLoader());
      int updatedPermissions = 0;   
      try 
      {
	      Resource [] permissionFiles = pmrl.getResources(jiraPermissionFilePattern);
	      
	      logger.debug("Found permissionsets: "+ permissionFiles.length);
	      
	      for (int i = 0; i < permissionFiles.length; i++)
	      {
		      PermissionScheme singleScheme = 
		         new ObjectMapper().readValue(
		    		permissionFiles[i].getInputStream(), PermissionScheme.class);
		      
		      String permissionSchemeName = project.key + " PERMISSION SCHEME";
		      
		      singleScheme.setName(permissionSchemeName);
		      
		      String description = project.description;
		      if (description != null && description.length() > 0) {
		    	  singleScheme.setDescription(description);
		      } else 
		      {
		    	  singleScheme.setDescription(permissionSchemeName);
		      }
		      
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
		    	  } 
		      }
		      logger.debug("Update permissionScheme " + permissionSchemeName +
		    	" location: " + permissionFiles[i].getFilename());
		      
		      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		      
		      String json = ow.writeValueAsString(singleScheme);
		      String path = String.format("%s%s/permissionscheme", jiraUri, jiraApiPath);
		      singleScheme = callHttp(path, json, crowdCookieValue, PermissionScheme.class, true, HTTP_VERB.POST);
		      
		      // update jira project
		      path = String.format("%s%s/project/%s/permissionscheme", jiraUri, jiraApiPath, project.key);
		      json = String.format("{ \"id\" : %s }", singleScheme.getId()); 
		      callHttp(path, json, crowdCookieValue, FullJiraProject.class, true, HTTP_VERB.PUT);
		      updatedPermissions++;
	      }
    	} catch (Exception createPermissions) 
    	{
  	    	// continue - we are ok if permissions fail, because the admin has access, and create the set
    		logger.error("Could not update permissionset: " + project.key + "\n Exception: " 
    				+ createPermissions.getMessage());
    	}      
      return updatedPermissions;
  }
  
  protected <T> T callHttp(String url, String json, String crowdCookieValue, Class valueType, 
		  boolean newClient, HTTP_VERB verb)
      throws IOException {
	  
    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);

    okhttp3.Request.Builder builder = new Request.Builder();
    builder.url(url).addHeader("X-Atlassian-Token", "no-check");
    
    if (HTTP_VERB.PUT.equals(verb))
    {
        builder = builder.put(body);
    } else if (HTTP_VERB.GET.equals(verb))
    {
    	builder = builder.get();
    } else if (HTTP_VERB.POST.equals(verb))
    {
    	builder = builder.post(body);
    }

    logger.debug("Call to: " + url + " :new:" + newClient);
    
    Response response = null;
    if (newClient)
    {
    	String credentials =
    			Credentials.basic(this.crowdUserDetailsService.loadUserByToken(crowdCookieValue).getUsername(),
    					manager.getUserPassword());
    	builder = builder.addHeader("Authorization", credentials);
    	response = client.getClientFresh(crowdCookieValue).newCall(builder.build()).execute();
    }
    else 
    {
    	response = client.getClient(crowdCookieValue).newCall(builder.build()).execute();	
    }
    	    
    String respBody = response.body().string();
    logger.debug(respBody);
    response.close();
    
    if (response.code() < 200 || response.code() >= 300)
    {
      throw new IOException(response.code()+": Could not " + verb + " > "  +valueType.getName() + ": " + respBody);
    }
    
    return (T)new ObjectMapper().readValue(respBody, valueType);
  }
  
  protected FullJiraProject buildJiraProjectPojoFromApiProject(ProjectData s) {
    BasicUser lead = s.admins.get(0);
    String type = "software";

    return new FullJiraProject(null, s.key, s.name, s.description, lead, null, null, null, null, null,
    	jiraTemplateKey, type);
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
    List<FullJiraProject> projects = getProjects(crowdCookieValue, key);
    for(FullJiraProject project : projects) {
      String normalizedProject = project.getKey().trim();
      String normalizedParam = key.trim();
      if(normalizedParam.equalsIgnoreCase(normalizedProject)) {
        return true;
      }
    }
    return false; 
  }

  // refactor - to only look for the project by key that is to be created!
  public List<FullJiraProject> getProjects(String crowdCookieValue, String filter) 
  {
    getSessionId();
    logger.debug("Getting jira projects with filter {}", filter);
    String url = 
    	filter == null || filter.trim().length() == 0 ? String.format("%s%s/project", jiraUri, jiraApiPath) :
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
  	return jiraUri + jiraApiPath;
  } 
  
  public int addShortcutsToProject (ProjectData data, String crowdCookieValue) 
  {
	if (!data.jiraconfluencespace) {
		return -1;
	}
	  
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String path = String.format("%s%s/project/%s/shortcut", jiraUri, jiraApiPath, data.key);

    List<Shortcut> shortcuts = new ArrayList<>();
    
    int id = 1;
    int createdShortcuts = 0;
    
	Shortcut shortcutConfluence = new Shortcut();
		shortcutConfluence.setId("" + id);
		shortcutConfluence.setName("Confluence: " + data.key);
		shortcutConfluence.setUrl(data.openshiftConsoleTestEnvUrl);
		shortcuts.add(shortcutConfluence);
		
	if (data.openshiftproject)
	{
	    Shortcut shortcutBB = new Shortcut();
	    	shortcutBB.setId("" + id++);
	    	shortcutBB.setName("GIT: " + data.key);
	    	shortcutBB.setUrl(data.bitbucketUrl);
	        shortcuts.add(shortcutBB);
	        
	    Shortcut shortcutJenkins = new Shortcut();
	    	shortcutJenkins.setId("" + id++);
	    	shortcutJenkins.setName("Jenkins");
	    	shortcutJenkins.setUrl(data.openshiftJenkinsUrl);
	        shortcuts.add(shortcutJenkins);
	
	    Shortcut shortcutOCDev = new Shortcut();
	    	shortcutOCDev.setId("" + id++);
	    	shortcutOCDev.setName("OC Dev " + data.key);
	    	shortcutOCDev.setUrl(data.openshiftConsoleDevEnvUrl);
	        shortcuts.add(shortcutOCDev);
	
		Shortcut shortcutOCTest = new Shortcut();
			shortcutOCTest.setId("" + id++);
			shortcutOCTest.setName("OC Test " + data.key);
			shortcutOCTest.setUrl(data.openshiftConsoleTestEnvUrl);
	        shortcuts.add(shortcutOCTest);
	}
	    	
	for (Shortcut shortcut : shortcuts) 
	{
		logger.debug("Attempting to creation shortcut for: " + shortcut.getName());
		try 
		{
		    String json = ow.writeValueAsString(shortcut);
		    callHttp(path, json, crowdCookieValue, Shortcut.class, false, HTTP_VERB.POST);
		    createdShortcuts++;
		} catch (IOException shortcutEx) 
		{
			logger.error("Could not create shortcut for: " + shortcut.getName() + 
				" Error: " + shortcutEx.getMessage());
		}
	}
	return createdShortcuts;
  }
}