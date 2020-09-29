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

import org.opendevstack.provision.config.JiraProjectTemplateProperties;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** @author Sebastian Titaks */
@Component
public class FullJiraProjectFactory {

  private static final Logger logger = LoggerFactory.getLogger(FullJiraProjectFactory.class);

  public static final String JIRA_TEMPLATE_KEY_PREFIX = "jira.project.template.key.";
  public static final String JIRA_TEMPLATE_TYPE_PREFIX = "jira.project.template.type.";

  @Value("${jira.project.template.key}")
  public String jiraTemplateKey;

  @Value("${jira.project.template.type}")
  public String jiraTemplateType;

  @Value("${project.template.default.key}")
  private String defaultProjectKey;

  @Value("${jira.project.notification.scheme.id:10000}")
  private String jiraNotificationSchemeId;

  @Autowired private JiraProjectTypePropertyCalculator jiraProjectTypePropertyCalculator;

  @Autowired private JiraProjectTemplateProperties jiraProjectTemplateProperties;

  public FullJiraProject buildJiraProjectPojoFromApiProject(OpenProjectData projectData) {
    String templateKey =
        jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                projectData.projectType, JIRA_TEMPLATE_KEY_PREFIX, jiraTemplateKey);

    String templateType =
        jiraProjectTypePropertyCalculator
            .readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
                projectData.projectType, JIRA_TEMPLATE_TYPE_PREFIX, jiraTemplateType);

    String permissionSchemeId = null;
    if (jiraProjectTemplateProperties
        .getProjectTemplates()
        .containsKey(projectData.getProjectType())) {
      permissionSchemeId =
          String.valueOf(
              jiraProjectTemplateProperties
                  .getProjectTemplates()
                  .get(projectData.getProjectType())
                  .getPermissionSchemeId());
      logger.info(
          "Found permission scheme id for project type [projectKey={}, projectType={}, permissionSchemeId={}]",
          projectData.projectKey,
          projectData.getProjectType(),
          permissionSchemeId);
    } else {
      logger.info(
          "No permission scheme id was setup for project type [projectKey={}, projectType={}]",
          projectData.projectKey,
          jiraTemplateKey);
    }

    if (jiraTemplateKey.equals(templateKey)) {
      // TODO: fix this... it is a side effect that could be difficult to find and debug!
      projectData.projectType = defaultProjectKey;
    }

    logger.debug(
        "Creating project of type: {} for project: {}", templateKey, projectData.projectKey);

    return new FullJiraProject(
        null,
        projectData.projectKey,
        projectData.projectName,
        projectData.description,
        projectData.projectAdminUser,
        templateKey,
        templateType,
        jiraNotificationSchemeId,
        permissionSchemeId);
  }
}
