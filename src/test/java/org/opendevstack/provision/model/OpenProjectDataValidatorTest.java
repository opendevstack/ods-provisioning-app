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

package org.opendevstack.provision.model;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_ID_KEY;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_TYPE_KEY;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenProjectDataValidatorTest {

  private OpenProjectData data;

  @BeforeEach
  public void setup() {
    data = new OpenProjectData();
    data.setProjectKey("KEY");
    data.setProjectName("Name");
    data.setDescription("Description");

    Map<String, String> someQuickstarter = new HashMap<>();
    someQuickstarter.put("key", "value");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(someQuickstarter);
    data.setQuickstarters(quickstarters);

    data.setPlatformRuntime(false);
    data.setSpecialPermissionSet(true);
    data.setProjectAdminUser("clemens");
    data.setProjectAdminGroup("group");
    data.setProjectUserGroup("group");
    data.setProjectReadonlyGroup("group");
  }

  @Test
  public void validComponentIdLength() {

    Consumer<Map<String, String>> validator = createComponentIdValidator();

    // case component id is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, null);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("null"));
    }

    // case component id is empty
    String empty = "";
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, empty);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("empty"));
    }

    // case component id longer than max length
    String tooLong =
        Strings.repeat("=", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH + 1);
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, tooLong);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      String errorMessage = e.getMessage();
      assertTrue(errorMessage.contains(tooLong));
      // special assert that verifies 2 errors: too long and not valid chars
      assertTrue(errorMessage.contains("not valid name"));
      //            assertEquals(2,
      // errorMessage.split(OpenProjectDataValidator.VALIDATION_ERROR_MESSAGE_SEPARATOR).length);
    }

    // case component id longer than max length
    String tooShort =
        Strings.repeat("=", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MIN_LENGTH - 1);
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, tooShort);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains(tooShort));
    }

    // case component id is longer or equal than max length
    String validLength =
        Strings.repeat("n", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH);
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, validLength);
    data.getQuickstarters().forEach(validator);
  }

  @Test
  public void validComponentIdNotEqualComponentType() {

    Consumer<Map<String, String>> validator =
        OpenProjectDataValidator.createComponentIdNotEqualComponentTypeValidator();

    // case component type is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, null);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("null"));
      assertTrue(e.getMessage().contains(COMPONENT_TYPE_KEY));
    }

    // case component id is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, "value1");
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, null);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("null"));
      assertTrue(e.getMessage().contains(COMPONENT_ID_KEY));
    }

    // case component type equals component id
    String same = "same";
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, same);
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, same);
      data.getQuickstarters().forEach(validator);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("is equal"));
    }

    // case component type not equals component id
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, "value1");
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, "value2");
    data.getQuickstarters().forEach(validator);
  }

  /**
   * Valid name starts with chars and can have dashes and alphanumerics in between and not be a
   * reserved keyword
   */
  @Test
  public void validComponentIdName() {
    validateIdName(
        createComponentIdValidator(),
        List.of(
            "invalid-contains.dot",
            "invalid-contains_underscore",
            "invalid-Contains-123-numbers",
            "tests"),
        List.of("valid-name", "valid-valid-NAME"));
  }

  @Test
  public void validProjectIdName() {
    validateIdName(
        createProjectIdValidator(),
        List.of("invalid-contains.dot", "invalid-contains_underscore"),
        List.of("valid-name", "valid-valid-NAME", "valid-23432-NAME", "valid-234-NAME323"));
  }

  public void validateIdName(
      Consumer<Map<String, String>> validator, List<String> invalidNames, List<String> validNames) {

    // case component id name cannot contains dot, underscore or be a reserved keyword
    invalidNames.forEach(
        notValid -> {
          try {
            data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, notValid);
            data.getQuickstarters().forEach(validator);
            fail(notValid + " is a not valid component id!");
          } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not valid name"));
          }
        });

    // case component id name cannot end with dash, dot or underscore
    List.of("Ends-with-dash-", "-starts-with-dash")
        .forEach(
            notValid -> {
              try {
                data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, notValid);
                data.getQuickstarters().forEach(validator);
                fail(notValid + " is a not valid component id!");
              } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("not valid name"));
              }
            });

    validNames.forEach(
        validName -> {
          data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, validName);
          data.getQuickstarters().forEach(validator);
        });
  }

  public Consumer<Map<String, String>> createComponentIdValidator() {
    return OpenProjectDataValidator.createComponentIdValidator(
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MIN_LENGTH,
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH);
  }

  public Consumer<Map<String, String>> createProjectIdValidator() {
    return OpenProjectDataValidator.createProjectIdValidator(
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MIN_LENGTH,
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH);
  }
}
