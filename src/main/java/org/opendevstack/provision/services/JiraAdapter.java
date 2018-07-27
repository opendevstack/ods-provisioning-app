package org.opendevstack.provision.services;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.atlassian.jira.rest.client.domain.BasicUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
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

  //Pattern to use for project with id
  private static String URL_PATTERN = "%s%s/project/%s";

  private static final MediaType JSON_MEDIA_TYPE = MediaType
      .parse("application/json; charset=utf-8");

  @Autowired
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  CustomAuthenticationManager manager;

  @Autowired
  RestClient client;

  private String crowdCookieValue = null;

  /**
   * Based on the information in the project object we create a jira project
   *
   * @param project data from the frontend
   * @param crowdCookieValue the value for the crowd cookie
   */
  public ProjectData createJiraProjectForProject(ProjectData project,
      String crowdCookieValue) throws IOException {
    try {
      this.crowdCookieValue = crowdCookieValue;
      client.getSessionId(jiraUri);

      logger.debug("Create new jira project");

      CrowdUserDetails details = crowdUserDetailsService.loadUserByToken(crowdCookieValue);
      project.admins.add(new BasicUser(null, details.getUsername(), details.getFullName()));

      FullJiraProject created = this
          .callJiraCreateProjectApi(this.buildJiraProjectPojoFromApiProject(project),
              crowdCookieValue);
      logger.debug("Created project: {}", created);
      created.getKey();
      project.jiraUrl = created.getSelf().toASCIIString();
      project.jiraId = created.id;
      return project;
      //return this.getProject(created.getKey(), crowdCookieValue);
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
    FullJiraProject created = this.post(path, json, crowdCookieValue);
    FullJiraProject returnProject = new FullJiraProject(created.getSelf(), created.getKey(),
        jiraProject.getName(), jiraProject.getDescription(), null, null, null, null, null, null,
        null, null);
    returnProject.id = created.id;
    return returnProject;
  }

  protected FullJiraProject post(String url, String json, String crowdCookieValue)
      throws IOException {

    CrowdUserDetails userDetails =
        (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    String username = userDetails.getUsername();
    String password = manager.getUserPassword();

    Request req = new Request.Builder().url(jiraUri).get().build();
    Response resp = client.getClient(crowdCookieValue).newCall(req).execute();
    Headers hds = resp.headers();

    //restTemplate.postForEntity(url, json)
    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
    Request request = new Request.Builder()
        .url(url)
        .post(body)
        //.header("Authorization", Credentials.basic(username, password))
        .build();

    Response response = client.getClient(crowdCookieValue).newCall(request).execute();
    String respBody = response.body().string();

    logger.debug(respBody);
    resp.close();
    response.close();
    
    if (response.code() < 200 || response.code() >= 300)
    {
      throw new IOException(response.code()+": Could not create jira project: " + respBody);
    }
    
    return new ObjectMapper().readValue(respBody, FullJiraProject.class);
  }

  protected FullJiraProject buildJiraProjectPojoFromApiProject(ProjectData s) {
    //String key = buildProjectKey(s.name);
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
    key = key.length() > 5 ? key.substring(0, 5) : key;
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
      
      return new ObjectMapper().readValue(respBody, new TypeReference<List<FullJiraProject>>() {});
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
