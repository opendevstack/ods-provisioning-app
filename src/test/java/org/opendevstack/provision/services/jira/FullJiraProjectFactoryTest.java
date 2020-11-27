/*
 * Copyright 2017-2019 the original author or authors.
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
package org.opendevstack.provision.services.jira;

import static org.junit.jupiter.api.Assertions.*;
import static org.opendevstack.provision.services.jira.JiraSpecialPermissionerTest.UTEST_CONFIGURED_PERMISSION_SCHEME_ID;
import static org.opendevstack.provision.services.jira.JiraSpecialPermissionerTest.UTEST_PROJECT_TEMPLATE_NAME;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("utest")
public class FullJiraProjectFactoryTest {

  public static final String UTEST_PROJECT_TEMPLATE_KEY = "utest-project-template-key";
  public static final String UTEST_PROJECT_TEMPLATE_TYPE = "utest-software";

  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;

  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @Value("${jira.project.notification.scheme.id:10000}")
  private String jiraNotificationSchemeId;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Autowired private FullJiraProjectFactory jiraProjectFactory;

  @Test
  public void testCreateFullJiraProjectWithPermissionSchemeId() {

    // case project template name is configured
    OpenProjectData project = createOpenProjectData("TEST1", UTEST_PROJECT_TEMPLATE_NAME);

    FullJiraProject jiraProject = jiraProjectFactory.buildJiraProjectPojoFromApiProject(project);

    assertTrue(jiraProject.hasPermissionSchemeId());
    assertEquals(
        Integer.valueOf(jiraProject.getPermissionScheme()), UTEST_CONFIGURED_PERMISSION_SCHEME_ID);
    assertEquals(jiraProject.getNotificationScheme(), jiraNotificationSchemeId);
    assertEquals(jiraProject.getProjectTemplateKey(), UTEST_PROJECT_TEMPLATE_KEY);
    assertEquals(jiraProject.getProjectTypeKey(), UTEST_PROJECT_TEMPLATE_TYPE);
    // asserting side effect until we refactor out the side effect
    assertNotEquals(project.getProjectType(), defaultProjectKey);
  }

  @Test
  public void testCreateFullJiraProjectWithDefaultTemplateKeyAndType() {

    // case project template name is not configured -> using default
    OpenProjectData project = createOpenProjectData("TEST2", "unexistant-project-template-name");

    FullJiraProject jiraProject = jiraProjectFactory.buildJiraProjectPojoFromApiProject(project);
    assertFalse(jiraProject.hasPermissionSchemeId());
    assertNull(jiraProject.getPermissionScheme());
    assertEquals(jiraProject.getProjectTemplateKey(), jiraTemplateKey);
    assertEquals(jiraProject.getProjectTypeKey(), jiraTemplateType);
    assertEquals(jiraProject.getNotificationScheme(), jiraNotificationSchemeId);
    // assserting side effect until we refactor out the side effect
    assertEquals(project.getProjectType(), defaultProjectKey);
  }

  @NotNull
  private OpenProjectData createOpenProjectData(String projectKey, String projectTemplateType) {
    OpenProjectData project = new OpenProjectData();
    project.setProjectKey(projectKey);
    project.projectType = projectTemplateType;
    return project;
  }
}
