package org.opendevstack.provision.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.jira.Permission;
import org.opendevstack.provision.model.jira.PermissionScheme;
import org.opendevstack.provision.model.jira.Shortcut;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

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

  public static final String JIRA_TEMPLATE_KEY_PREFIX = "jira.project.template.key.";
  public static final String JIRA_TEMPLATE_TYPE_PREFIX = "jira.project.template.type.";
  
  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;
  
  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @Value("${jira.project.notification.scheme.id:10000}")
  String jiraNotificationSchemeId;
  
  //Pattern to use for project with id
  private static final String URL_PATTERN = "%s%s/project/%s";

  @Autowired
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  CustomAuthenticationManager manager;

  @Value("${global.keyuser.role.name}")
  private String globalKeyuserRoleName;
    
  @Autowired
  RestClient client;

  @Autowired
  ConfigurableEnvironment environment;

  @Autowired
  List<String> projectTemplateKeyNames;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;
    
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
      
      FullJiraProject toBeCreated = 
    		  this.buildJiraProjectPojoFromApiProject(project);
      
      FullJiraProject created = null;
      try {
    	  created = this.callJiraCreateProjectApi(toBeCreated,crowdCookieValue); 
      } catch (HttpException jiracreateException) 
      {
          logger.debug("error creating project with template {}: {}", 
        		  toBeCreated.projectTemplateKey,
        		  jiracreateException.getMessage());
    	  if (jiracreateException.getResponseCode() == 400) {
    		  logger.info("Template {} did not work, falling back to default {}", 
    				 toBeCreated.projectTemplateKey, jiraTemplateKey);
    		  toBeCreated.projectTypeKey = jiraTemplateType;
    		  toBeCreated.projectTemplateKey = jiraTemplateKey;
        	  created = this.callJiraCreateProjectApi(toBeCreated,crowdCookieValue);
        	  project.projectType = defaultProjectKey;
    	  } else 
    	  {
    		  throw jiracreateException;
    	  }
      }
          
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
      try {
    	  return client.callHttp(url, null, crowdCookieValue, false, RestClient.HTTP_VERB.GET, FullJiraProject.class);
      } catch (IOException eGetProjects) 
      {
        logger.error("Error getting projects:", eGetProjects);
        return null;
      }

  }

  protected FullJiraProject callJiraCreateProjectApi(
      FullJiraProject jiraProject, String crowdCookieValue)
      throws IOException 
  {
    String path = String.format("%s%s/project", jiraUri, jiraApiPath);
	  
	FullJiraProject created = client.callHttp(path, jiraProject, 
			crowdCookieValue, false, RestClient.HTTP_VERB.POST, FullJiraProject.class);  
    FullJiraProject returnProject = new FullJiraProject(created.getSelf(), created.getKey(),
        jiraProject.getName(), jiraProject.getDescription(), null, null, null, null, null, null,
        null, null, null);
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
	      
	      logger.debug("Found permissionsets: {}", permissionFiles.length);

          for (Resource permissionFile : permissionFiles) {
              PermissionScheme singleScheme =
                      new ObjectMapper().readValue(
                              permissionFile.getInputStream(), PermissionScheme.class);

              String permissionSchemeName = project.key + " PERMISSION SCHEME";

              singleScheme.setName(permissionSchemeName);

              String description = project.description;
              if (description != null && description.length() > 0) {
                  singleScheme.setDescription(description);
              } else {
                  singleScheme.setDescription(permissionSchemeName);
              }

              // replace group with real group
              for (Permission permission : singleScheme.getPermissions()) {
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
              logger.debug("Update permissionScheme {} location: {}",
                      permissionSchemeName, permissionFile.getFilename());

              String path = String.format("%s%s/permissionscheme", jiraUri, jiraApiPath);
              singleScheme =
                      client.callHttp(path, singleScheme, crowdCookieValue, true,
                              RestClient.HTTP_VERB.POST, PermissionScheme.class);

              // update jira project
              path = String.format("%s%s/project/%s/permissionscheme", jiraUri, jiraApiPath, project.key);
              PermissionScheme small = new PermissionScheme();
              small.setId(singleScheme.getId());
              client.callHttp(path, small, crowdCookieValue, true, RestClient.HTTP_VERB.PUT, FullJiraProject.class);

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
  
  protected FullJiraProject buildJiraProjectPojoFromApiProject(ProjectData s) {
    BasicUser lead = s.admins.get(0);

    String templateKey = calculateJiraProjectTypeAndTemplateFromProjectType
        (s, JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey);
    		
    String templateType = calculateJiraProjectTypeAndTemplateFromProjectType
    	(s, JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType);
    
    if (jiraTemplateKey.equals(templateKey)) 
    {
    	s.projectType = defaultProjectKey;
    }
    		
    logger.debug("Creating project of type: {} for project: {}", templateKey, s.key);

    return new FullJiraProject(null, s.key, s.name, s.description, lead, null, null, null, null, null,
    	templateKey, templateType, jiraNotificationSchemeId);
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
  public List<FullJiraProject> getProjects(String crowdCookieValue, String filter) {
      getSessionId();
      logger.debug("Getting jira projects with filter {}", filter);
      String url =
              filter == null || filter.trim().length() == 0 ? String.format("%s%s/project", jiraUri, jiraApiPath) :
                      String.format(URL_PATTERN, jiraUri, jiraApiPath, filter);
      try {
          return client.callHttpTypeRef(url, null, crowdCookieValue, false, RestClient.HTTP_VERB.GET,
                  new TypeReference<List<FullJiraProject>>() {
                  });
      } catch (JsonMappingException e) {
          // if for some odd reason serialization fails ...
          List<FullJiraProject> returnList = new ArrayList<>();
          returnList.add(new FullJiraProject
                  (null, filter, filter, filter, null, null, null, null, null, null, null, null, null));

          return returnList;
      } catch (HttpException e) {
          if (e.getResponseCode() != 404) {
              logger.error("Error in getting projects", e);
          }
          // if for nothing else -
          return new ArrayList<>();
      } catch (IOException e) {
          return new ArrayList<>();
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
	  
    String path = String.format("%s/rest/projects/1.0/project/%s/shortcut", jiraUri, data.key);

    List<Shortcut> shortcuts = new ArrayList<>();
    
    int id = 1;
    int createdShortcuts = 0;
    
	Shortcut shortcutConfluence = new Shortcut();
		shortcutConfluence.setId("" + id++);
		shortcutConfluence.setName("Confluence: " + data.key);
		shortcutConfluence.setUrl(data.confluenceUrl);
		shortcutConfluence.setIcon("");
		shortcuts.add(shortcutConfluence);
		
	if (data.openshiftproject)
	{
	    Shortcut shortcutBB = new Shortcut();
	    	shortcutBB.setId("" + id++);
	    	shortcutBB.setName("GIT: " + data.key);
	    	shortcutBB.setUrl(data.bitbucketUrl);
	    	shortcutBB.setIcon("");
	        shortcuts.add(shortcutBB);
	        
	    Shortcut shortcutJenkins = new Shortcut();
	    	shortcutJenkins.setId("" + id++);
	    	shortcutJenkins.setName("Jenkins");
	    	shortcutJenkins.setUrl(data.openshiftJenkinsUrl);
	    	shortcutJenkins.setIcon("");
	        shortcuts.add(shortcutJenkins);
	
	    Shortcut shortcutOCDev = new Shortcut();
	    	shortcutOCDev.setId("" + id++);
	    	shortcutOCDev.setName("OC Dev " + data.key);
	    	shortcutOCDev.setUrl(data.openshiftConsoleDevEnvUrl);
	    	shortcutOCDev.setIcon("");
	        shortcuts.add(shortcutOCDev);
	
		Shortcut shortcutOCTest = new Shortcut();
			shortcutOCTest.setId("" + id);
			shortcutOCTest.setName("OC Test " + data.key);
			shortcutOCTest.setUrl(data.openshiftConsoleTestEnvUrl);
			shortcutOCTest.setIcon("");
	        shortcuts.add(shortcutOCTest);
	}
	    	
	for (Shortcut shortcut : shortcuts) 
	{
		logger.debug("Attempting to create shortcut ({}) for: {}",
                shortcut.getId(), shortcut.getName());
		try 
		{
			client.callHttp(path, shortcut, crowdCookieValue, false, RestClient.HTTP_VERB.POST, Shortcut.class);
		    createdShortcuts++;
		} catch (IOException shortcutEx) 
		{
			logger.error("Could not create shortcut for: " + shortcut.getName() + 
				" Error: " + shortcutEx.getMessage());
		}
	}
	return createdShortcuts;
  }

  public String calculateJiraProjectTypeAndTemplateFromProjectType 
  	(ProjectData project, String templatePrefix, String defaultValue) 
  {
	  Preconditions.checkNotNull(templatePrefix, "no template prefix passed");
	  Preconditions.checkNotNull(defaultValue, "no defaultValue passed");
	  /*
	   * if the type can be found in the global definition of types (projectTemplateKeyNames)
	   * and is also configured for jira (environment.containsProperty) - take it, if not
	   * fall back to default
	   */
	  return
    	(project.projectType != null && 
    		environment.containsProperty(templatePrefix + project.projectType) && 
    		projectTemplateKeyNames.contains(project.projectType)) ?
    		environment.getProperty(templatePrefix + project.projectType) : defaultValue;
	  
  }
  
}