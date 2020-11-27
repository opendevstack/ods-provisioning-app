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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.services.JiraAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("utest")
public class JiraProjectTypePropertyCalculatorTest {

  public static final String DISABLED_TEMPLATE = "disabled-template";
  public static final String DEFAULT_VALUE = "defaultValue";
  public static final String KANBAN = "kanban";
  public static final String UNKNOWN_PROJECT_TYPE = "unknownProjectType";

  @Autowired private Environment environment;

  @Autowired private JiraProjectTypePropertyCalculator propertyCalculator;

  @BeforeEach
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void givenTemplatePrefixOrDefaultValue_whenOneIsNull_ThenException() {

    try {
      propertyCalculator.readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
          null, null, "not-null");
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("template prefix"));
    }

    try {
      propertyCalculator.readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
          null, "not-null", null);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("defaultValue"));
    }
  }

  @Test
  public void givenProjectType_whenTypeIsEnabled_ThenReturnValuesFromConfiguration() {

    // case 1: environment contain template prefix + project type and project type is one of the
    // available template key
    // then should return property "template prefix + project type"
    String kanban = KANBAN;
    String value =
        propertyCalculator.readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
            kanban, JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, DEFAULT_VALUE);
    assertEquals(environment.getProperty(JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX + kanban), value);
  }

  @Test
  public void givenProjectType_whenTypeNotEnabled_ThenReturnDefalutValue() {

    // case 1: environment does not contain template prefix + project type and project type is one
    // of the available template key
    String unknownProjectType = UNKNOWN_PROJECT_TYPE;
    String value =
        propertyCalculator.readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
            unknownProjectType, JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, DEFAULT_VALUE);
    assertEquals(DEFAULT_VALUE, value);

    // case 2: environment contain template prefix + project type but project type is not one of the
    // available template key
    String disabledTemplate = DISABLED_TEMPLATE;
    value =
        propertyCalculator.readPropertyIfTemplateKeyExistsAndIsEnabledOrReturnDefault(
            disabledTemplate, JiraAdapter.JIRA_TEMPLATE_TYPE_PREFIX, DEFAULT_VALUE);
    assertEquals(DEFAULT_VALUE, value);
  }
}
