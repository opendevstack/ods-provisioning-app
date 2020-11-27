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

package org.opendevstack.provision.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class CheckPreconditionsResponseTest {

  private static Logger logger = LoggerFactory.getLogger(CheckPreconditionsResponseTest.class);

  @Test
  public void failedResponseAsJson() {

    String expectedJson =
        "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CREATE_PROJECT\",\"status\":\"FAILED\",\"errors\":[{\"error-code\":\"EXCEPTION\",\"error-message\":\"error1\"}]}";

    assertEquals(
        expectedJson,
        CheckPreconditionsResponse.failedAsJson(
            CheckPreconditionsResponse.JobStage.CREATE_PROJECT, "error1"));
  }

  @Test
  public void failedResponse() throws JsonProcessingException {

    List<CheckPreconditionFailure> errors = new ArrayList<>();
    errors.add(CheckPreconditionFailure.getUnexistantGroupInstance("failure1"));
    errors.add(CheckPreconditionFailure.getUnexistantUserInstance("failure2"));

    assertEquals(
        "CHECK_PRECONDITIONS=FAILED"
            + System.lineSeparator()
            + "ERRORS=[CheckPreconditionFailure{error-code='UNEXISTANT_GROUP', detail='failure1'}, CheckPreconditionFailure{error-code='UNEXISTANT_USER', detail='failure2'}]"
            + System.lineSeparator(),
        CheckPreconditionsResponse.checkPreconditionFailed(
            null, CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS, errors));

    String expectedJson =
        "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"FAILED\",\"errors\":[{\"error-code\":\"UNEXISTANT_GROUP\",\"error-message\":\"failure1\"},{\"error-code\":\"UNEXISTANT_USER\",\"error-message\":\"failure2\"}]}";

    assertEquals(
        expectedJson,
        CheckPreconditionsResponse.checkPreconditionFailed(
            MediaType.APPLICATION_JSON_VALUE,
            CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS,
            errors));
  }

  @Test
  public void isJsonContentType() {

    assertFalse(CheckPreconditionsResponse.isJsonContentType(null));

    assertFalse(CheckPreconditionsResponse.isJsonContentType("something"));

    assertTrue(CheckPreconditionsResponse.isJsonContentType(MediaType.APPLICATION_JSON_VALUE));

    assertTrue(
        CheckPreconditionsResponse.isJsonContentType(
            MediaType.APPLICATION_JSON_VALUE + "; " + " more text"));
  }

  @Test
  public void successfulResponse() throws JsonProcessingException {

    assertEquals(
        "CHECK_PRECONDITIONS=COMPLETED_SUCCESSFULLY",
        CheckPreconditionsResponse.successful(
            null, CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS));

    String expectedJson =
        "{\"endpoint\":\"ADD_PROJECT\",\"stage\":\"CHECK_PRECONDITIONS\",\"status\":\"COMPLETED_SUCCESSFULLY\"}";
    assertEquals(
        expectedJson,
        CheckPreconditionsResponse.successful(
            MediaType.APPLICATION_JSON_VALUE,
            CheckPreconditionsResponse.JobStage.CHECK_PRECONDITIONS));
  }
}
