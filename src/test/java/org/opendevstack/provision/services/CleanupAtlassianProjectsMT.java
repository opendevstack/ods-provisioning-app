package org.opendevstack.provision.services;

import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringBoot.class)
@ActiveProfiles("testods")
@Ignore("Only exists for manual execution")
public class CleanupAtlassianProjectsMT {

  private static final Logger LOG = LoggerFactory.getLogger(CleanupAtlassianProjectsMT.class);

  @Autowired private RestClient restClient;

  @Autowired private JiraAdapter jiraAdapter;

  @Autowired private Environment environment;

  private CredentialsInfo jiraCredentials;
  private CredentialsInfo confluenceCredentials;
  private CredentialsInfo bitbucketCredentials;

  @Value("${bitbucket.uri}")
  private String bitbucketUrl;

  @Value("${jira.uri}")
  private String jiraUrl;

  @Value("${confluence.uri}")
  private String confluenceUrl;

  @Before
  public void setUp() {
    jiraCredentials = buildCredentials("jira");
    confluenceCredentials = buildCredentials("confluence");
    bitbucketCredentials = buildCredentials("bitbucket");
  }

  @Test
  public void cleanupJiraProjects() {
    LOG.info("Found jira projects " + String.join(",", jiraAdapter.getProjects(null).keySet()));
    deleteJiraProjects("x1", "x2");
  }

  private CredentialsInfo buildCredentials(String configurationPrefix) {
    String propertyAdminUserKey = configurationPrefix + ".admin_user";
    String propertyAdminUserPasswordKey = configurationPrefix + ".admin_password";
    return new CredentialsInfo(
        environment.getProperty(propertyAdminUserKey),
        environment.getProperty(propertyAdminUserPasswordKey));
  }

  @Test
  public void cleanupConfluence() throws IOException {
    deleteConfluenceSpaces("x1", "x2");
  }

  @Test
  public void cleanupBitbucketProjects() {
    deleteBitbucketProjects("x1", "x2");
  }

  private void deleteBitbucketProjects(String... keys) {
    Arrays.stream(keys).forEach(this::deleteBitbucketProject);
  }

  private void deleteBitbucketProject(String projectKey) {
    if (projectKey.equals("OPENDEVSTACK")) {
      LOG.warn("Project " + projectKey + " will not be deleted!");
      return;
    }
    List<String> reproSlugs = readBitbucketRepoSlugs(projectKey);
    reproSlugs.forEach(slug -> deleteBitbucketRepo(projectKey, slug));
    executeDeleteOperation(
        this.bitbucketCredentials, "%s/rest/api/1.0/projects/%s", bitbucketUrl, projectKey);
  }

  private List<String> readBitbucketRepoSlugs(String projectKey) {
    List<String> reproSlugs = Collections.emptyList();
    RestClientCall call =
        RestClientCall.get()
            .url("%s/rest/api/1.0/projects/%s/repos?limit=1000", bitbucketUrl, projectKey)
            .returnTypeReference(new TypeReference<List<JsonNode>>() {})
            .basicAuthenticated(bitbucketCredentials);

    try {
      reproSlugs =
          restClient.<List<JsonNode>>execute(call).get(0).path("values").findValuesAsText("slug");
    } catch (IOException e) {
      LOG.warn(e.getMessage());
    }

    return reproSlugs;
  }

  private void deleteBitbucketRepo(String projektKey, String reproSlug) {
    RestClientCall call =
        RestClientCall.delete()
            .url("%s/rest/api/1.0/projects/%s/repos/%s", bitbucketUrl, projektKey, reproSlug)
            .basicAuthenticated(bitbucketCredentials);
    try {
      restClient.execute(call);
    } catch (IOException e) {
      fail("Cannot delete repo " + reproSlug + " " + e.getMessage());
    }
  }

  private void deleteConfluenceSpaces(String... keys) {
    Arrays.stream(keys).forEach(this::deleteConfluenceSpace);
  }

  private void deleteConfluenceSpace(String projectKey) {
    if (projectKey.equals("ds") || projectKey.equals("OP")) {
      LOG.warn("Confluence space " + projectKey + " will be not deleted");
      return;
    }

    executeDeleteOperation(
        this.confluenceCredentials, "%s/rest/api/latest/space/%s", confluenceUrl, projectKey);
  }

  private void deleteJiraProjects(String... keys) {
    Arrays.stream(keys).forEach(this::deleteJiraProject);
  }

  private void deleteJiraProject(String projectKey) {
    executeDeleteOperation(
        this.jiraCredentials, "%s/rest/api/latest/project/%s", jiraUrl, projectKey);
  }

  private void executeDeleteOperation(
      CredentialsInfo credentials, String urlFormat, Object... args) {
    RestClientCall call =
        RestClientCall.delete().basicAuthenticated(credentials).url(urlFormat, args);
    try {
      restClient.execute(call);
    } catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }
}
