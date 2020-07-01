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

import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_ID_KEY;
import static org.opendevstack.provision.model.OpenProjectData.COMPONENT_TYPE_KEY;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** @author Sebastian Titakis */
public class OpenProjectDataValidatorTest {

  private OpenProjectData data;

  @Before
  public void setup() {
    data = new OpenProjectData();
    data.projectKey = "KEY";
    data.projectName = "Name";
    data.description = "Description";

    Map<String, String> someQuickstarter = new HashMap<>();
    someQuickstarter.put("key", "value");
    List<Map<String, String>> quickstarters = new ArrayList<>();
    quickstarters.add(someQuickstarter);
    data.quickstarters = quickstarters;

    data.platformRuntime = false;
    data.specialPermissionSet = true;
    data.projectAdminUser = "clemens";
    data.projectAdminGroup = "group";
    data.projectUserGroup = "group";
    data.projectReadonlyGroup = "group";
  }

  @Test
  public void validComponentIdLength() {

    Consumer<Map<String, String>> validator = createComponentIdValidator();

    // case component id is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, null);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("null"));
    }

    // case component id is empty
    String empty = "";
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, empty);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("empty"));
    }

    // case component id longer than max length
    String tooLong =
        Strings.repeat("=", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH + 1);
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, tooLong);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      String errorMessage = e.getMessage();
      Assert.assertTrue(errorMessage.contains(tooLong));
      // special assert that verifies 2 errors: too long and not valid chars
      Assert.assertTrue(errorMessage.contains("not valid name"));
      //            Assert.assertEquals(2,
      // errorMessage.split(OpenProjectDataValidator.VALIDATION_ERROR_MESSAGE_SEPARATOR).length);
    }

    // case component id longer than max length
    String tooShort =
        Strings.repeat("=", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MIN_LENGTH - 1);
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, tooShort);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains(tooShort));
    }

    // case component id is longer or equal than max length
    String validLength =
        Strings.repeat("n", OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH);
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, validLength);
    data.quickstarters.forEach(validator);
  }

  @Test
  public void validComponentIdNotEqualComponentType() {

    Consumer<Map<String, String>> validator =
        OpenProjectDataValidator.createComponentIdNotEqualComponentTypeValidator();

    // case component type is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, null);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("null"));
      Assert.assertTrue(e.getMessage().contains(COMPONENT_TYPE_KEY));
    }

    // case component id is null
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, "value1");
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, null);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("null"));
      Assert.assertTrue(e.getMessage().contains(COMPONENT_ID_KEY));
    }

    // case component type equals component id
    String same = "same";
    try {
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, same);
      data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, same);
      data.quickstarters.forEach(validator);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("is equal"));
    }

    // case component type not equals component id
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, "value1");
    data.getQuickstarters().stream().findFirst().get().put(COMPONENT_TYPE_KEY, "value2");
    data.quickstarters.forEach(validator);
  }

  /** Valid name starts with chars and can have dashes and alphanumerics in between */
  @Test
  public void validComponentIdName() {

    Consumer<Map<String, String>> validator = createComponentIdValidator();

    // case component id name cannot contains dot or underscore
    List.of("contains.dot", "contains_underscore", "Contains-123-numbers")
        .forEach(
            notValid -> {
              try {
                data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, notValid);
                data.quickstarters.forEach(validator);
                Assert.fail(notValid + " is a not valid component id!");
              } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage().contains("not valid name"));
              }
            });

    // case component id name cannot end with dash, dot or underscore
    List.of("Ends-with-dash-", "-starts-with-dash")
        .forEach(
            notValid -> {
              try {
                data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, notValid);
                data.quickstarters.forEach(validator);
                Assert.fail(notValid + " is a not valid component id!");
              } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage().contains("not valid name"));
              }
            });

    // case component valid name
    List.of("valid-name", "valid-valid-NAME")
        .forEach(
            validName -> {
              data.getQuickstarters().stream().findFirst().get().put(COMPONENT_ID_KEY, validName);
              data.quickstarters.forEach(validator);
            });
  }

  public Consumer<Map<String, String>> createComponentIdValidator() {
    return OpenProjectDataValidator.createComponentIdValidator(
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MIN_LENGTH,
        OpenProjectDataValidator.API_ALLOWED_COMPONENT_ID_MAX_LENGTH);
  }
}
