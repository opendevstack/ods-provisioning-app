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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Provides validators method
 *
 * @author: Sebastian Titakis
 */
public class OpenProjectDataValidator {
  /**
   * Valid name starts with alpha chars and only dashes (-) in between are allowed. It is more
   * restrictive than kubernetes label syntax
   *
   * @see <a href="kubernetes label syntax and character
   *     set">https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set</a>
   */
  public static final String COMPONENT_ID_VALIDATOR_REGEX =
      "^(?!tests)(([A-Za-z][-A-Za-z]*)?[A-Za-z])?$";

  public static final String PROJECT_ID_VALIDATOR_REGEX =
      "^(([A-Za-z][-A-Za-z0-9]*)?[A-Za-z0-9])?$";

  public static final String VALIDATION_ERROR_MESSAGE_SEPARATOR = "//";

  private static final PatternErrorMessage componentIdRegex =
      new PatternErrorMessage(
          Pattern.compile(COMPONENT_ID_VALIDATOR_REGEX),
          " is not valid name (only alpha chars are allowed with dashes (-) allowed in between. Reserved keywords e.g, \"tests\" are not allowed.");

  private static final PatternErrorMessage projectIdRegex =
      new PatternErrorMessage(
          Pattern.compile(PROJECT_ID_VALIDATOR_REGEX),
          " is not valid name (only alpha chars are allowed with dashes (-) and numbers allowed in between. Numbers at the end are allowed too!");

  public static final int COMPONENT_ID_MIN_LENGTH = 3;
  public static final int COMPONENT_ID_MAX_LENGTH = 63;

  public static final int API_ALLOWED_COMPONENT_ID_MIN_LENGTH =
      OpenProjectDataValidator.COMPONENT_ID_MIN_LENGTH;
  public static final int API_ALLOWED_COMPONENT_ID_MAX_LENGTH = 40;

  private static final String COMPONENT_ID_KEY = OpenProjectData.COMPONENT_ID_KEY;
  private static final String COMPONENT_TYPE_KEY = OpenProjectData.COMPONENT_TYPE_KEY;

  public static final List<Consumer<Map<String, String>>> API_COMPONENT_ID_VALIDATOR_LIST =
      Arrays.asList(
          OpenProjectDataValidator.createComponentIdValidator(
              API_ALLOWED_COMPONENT_ID_MIN_LENGTH, API_ALLOWED_COMPONENT_ID_MAX_LENGTH),
          OpenProjectDataValidator.createComponentIdNotEqualComponentTypeValidator());

  public static Consumer<Map<String, String>> createComponentIdValidator(
      int minLength, int maxLength) {
    return createIdValidator(componentIdRegex, minLength, maxLength);
  }

  public static Consumer<Map<String, String>> createProjectIdValidator(
      int minLength, int maxLength) {
    return createIdValidator(projectIdRegex, minLength, maxLength);
  }

  private static Consumer<Map<String, String>> createIdValidator(
      PatternErrorMessage pattern, int minLength, int maxLength) {

    return quickstarter -> {
      String componentId = quickstarter.get(COMPONENT_ID_KEY);
      if (componentId == null) {
        throw new IllegalArgumentException(COMPONENT_ID_KEY + " is null!");
      } else if (componentId.trim().length() == 0) {
        throw new IllegalArgumentException(COMPONENT_ID_KEY + " '" + componentId + "' is empty!");
      }

      List<String> errors = new ArrayList<>();
      if (componentId.length() < minLength) {
        errors.add(
            COMPONENT_ID_KEY + " '" + componentId + "' is shorter than " + minLength + " chars!");
      }

      if (componentId.length() > maxLength) {
        errors.add(
            COMPONENT_ID_KEY + " '" + componentId + "' is longer than " + maxLength + " chars!");
      }

      if (!pattern.getPattern().matcher(componentId).find()) {
        errors.add(COMPONENT_ID_KEY + " '" + componentId + "' " + pattern.getErrorMessage());
      }

      if (!errors.isEmpty()) {
        String message = String.join(VALIDATION_ERROR_MESSAGE_SEPARATOR, errors);
        throw new IllegalArgumentException(message);
      }
    };
  }

  public static Consumer<Map<String, String>> createComponentIdNotEqualComponentTypeValidator() {

    return quickstarter -> {
      String componentType = quickstarter.get(COMPONENT_TYPE_KEY);
      String componentId = quickstarter.get(COMPONENT_ID_KEY);
      if (componentType == null) {
        throw new IllegalArgumentException(COMPONENT_TYPE_KEY + " is null!");
      } else if (componentId == null) {
        throw new IllegalArgumentException(COMPONENT_ID_KEY + " is null!");
      } else if (componentId.trim().equals(componentType)) {
        throw new IllegalArgumentException(
            COMPONENT_ID_KEY
                + " '"
                + componentId
                + "' is equals as component_type '"
                + componentType
                + "'!");
      }
    };
  }

  private static class PatternErrorMessage {

    private final Pattern pattern;

    private final String errorMessage;

    public PatternErrorMessage(Pattern pattern, String errorMessage) {
      this.pattern = pattern;
      this.errorMessage = errorMessage;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
