package org.opendevstack.provision.services.jira;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("utest")
public class JiraProjectPropertyUpdaterTest {

  public static final String UTEST_JIRA_PROPERTY_EXAMPLE_ENDPOINT =
      "jira.project.template.key.example.endpoint.";
  public static final String UTEST_JIRA_PROPERTY_EXAMPLE_PAYLOAD =
      "jira.project.template.key.example.payload.";
  public static final String PROJECT_TEMPLATE_NAME = "utest-project-template";

  @Value("${jira.uri}")
  private String jiraUri;

  @Autowired private JiraProjectPropertyUpdater projectPropertyUpdater;

  @Test
  public void
      givenSetPropertyInJiraProject_whenTemplateOrPayloadConfigPropertiesAreNotValid_thenRuntimeException()
          throws IOException {

    JiraRestService jiraRestService = mock(JiraRestService.class);

    String projectKey = "PROJECTKEY";

    try {
      projectPropertyUpdater.setPropertyInJiraProject(
          jiraRestService,
          projectKey,
          PROJECT_TEMPLATE_NAME,
          "VALUE",
          "not-valid-config-property.",
          UTEST_JIRA_PROPERTY_EXAMPLE_PAYLOAD);
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("No jira endpoint is defined"));
    }

    try {
      projectPropertyUpdater.setPropertyInJiraProject(
          jiraRestService,
          projectKey,
          PROJECT_TEMPLATE_NAME,
          "VALUE",
          UTEST_JIRA_PROPERTY_EXAMPLE_ENDPOINT,
          "not-valid-config-property.");
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("No jira endpoint payload is defined"));
    }
  }

  @Test
  public void givenSetPropertyInJiraProject_whenAllParamsAreValid_thenExecutesRestClientCall()
      throws IOException {

    JiraRestService jiraRestService = mock(JiraRestService.class);
    RestClient jiraClient = mock(RestClient.class);
    RestClientCall restClientCall = mock(RestClientCall.class);
    when(jiraRestService.httpPost()).thenReturn(restClientCall);
    when(restClientCall.url(anyString())).thenReturn(restClientCall);
    when(restClientCall.body(anyString())).thenReturn(restClientCall);
    when(restClientCall.header(any())).thenReturn(restClientCall);
    when(jiraRestService.getRestClient()).thenReturn(jiraClient);

    String projectKey = "PROJECTKEY";

    projectPropertyUpdater.setPropertyInJiraProject(
        jiraRestService,
        projectKey,
        PROJECT_TEMPLATE_NAME,
        "VALUE",
        UTEST_JIRA_PROPERTY_EXAMPLE_ENDPOINT,
        UTEST_JIRA_PROPERTY_EXAMPLE_PAYLOAD);

    verify(jiraRestService, times(1)).httpPost();
    verify(restClientCall, times(1))
        .url(
            argThat(
                new ArgumentMatcher<String>() {
                  @Override
                  public boolean matches(String argument) {
                    return argument.startsWith(jiraUri);
                  }
                }));
    verify(restClientCall, times(1)).body(anyString());
    verify(restClientCall, times(1))
        .header(eq(JiraProjectPropertyUpdater.HTTP_HEADERS_CONTENT_TYPE_JSON_AND_ACCEPT_JSON));
    verify(jiraClient, times(1)).execute(restClientCall);
  }

  @Test
  public void givenParseTemplate_WhenKeyAndValueValid_ThenTemplateParsedProperly() {

    String key = "key";
    String value = "value";
    String template =
        JiraProjectPropertyUpdater.JIRA_PROJECT_KEY
            + "="
            + JiraProjectPropertyUpdater.JIRA_PROJECT_PROPERTY_VALUE;
    String result = JiraProjectPropertyUpdater.parseTemplate(key, value, template);
    assertEquals("key=value", result);
  }
}
