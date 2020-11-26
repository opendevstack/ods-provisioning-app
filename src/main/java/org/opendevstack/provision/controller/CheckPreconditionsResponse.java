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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class CheckPreconditionsResponse {

  private static Logger logger = LoggerFactory.getLogger(CheckPreconditionsResponse.class);

  public static final String API_NAME = "ADD_PROJECT";
  public static final String ERRORS_KEY = "ERRORS";
  public static final String KEY_VALUE_SEPARATOR = "=";
  public static final String ERRORS_DELIMITER = ",";

  public enum JobStage {
    INPUT_VALIDATION,
    CHECK_PRECONDITIONS,
    CREATE_PROJECT
  }

  public enum JobStatus {
    FAILED,
    COMPLETED_SUCCESSFULLY
  }

  private CheckPreconditionsResponse() {}

  public static String checkPreconditionFailed(
      String contentType, JobStage jobStage, List<CheckPreconditionFailure> preconditionsFailures)
      throws JsonProcessingException {

    if (isJsonContentType(contentType)) {
      return new CheckPreconditionFailedResponse(jobStage, preconditionsFailures).asJson();
    } else {
      return new CheckPreconditionFailedResponse(jobStage, preconditionsFailures).asText();
    }
  }

  /**
   * @param jobStage
   * @param error
   * @return empty json is an exception happens when parsing from object to json string
   */
  public static String failedAsJson(JobStage jobStage, String error) {

    try {
      return new CheckPreconditionFailedResponse(
              jobStage, List.of(CheckPreconditionFailure.getExceptionInstance(error)))
          .asJson();
    } catch (JsonProcessingException e) {
      logger.warn("This should never happen, returning empty json payload!", e);
      return "{}";
    }
  }

  public static boolean isJsonContentType(String contentType) {
    return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
  }

  public static String successful(String contentType, JobStage jobStage)
      throws JsonProcessingException {
    SuccessfulResponse response = new SuccessfulResponse(jobStage);
    return isJsonContentType(contentType) ? response.asJson() : response.asText();
  }

  private static class JobResponse {

    @JsonProperty("endpoint")
    private String endpoint = API_NAME;

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("status")
    private String status;

    private ObjectMapper mapper = new ObjectMapper();

    private JobResponse(JobStage stage, JobStatus status) {
      this.stage = stage.name();
      this.status = status.name();
    }

    public final String asJson() throws JsonProcessingException {
      return mapper.writeValueAsString(this);
    }

    public final String getStage() {
      return stage;
    }

    public final String getStatus() {
      return status;
    }
  }

  private static class SuccessfulResponse extends JobResponse {

    private SuccessfulResponse(JobStage jobStage) {
      super(jobStage, JobStatus.COMPLETED_SUCCESSFULLY);
    }

    public String asText() {
      return getStage() + KEY_VALUE_SEPARATOR + getStatus();
    }
  }

  private static class CheckPreconditionFailedResponse extends JobResponse {

    @JsonProperty("errors")
    private List<CheckPreconditionFailure> errorDetails = new ArrayList<>();

    private CheckPreconditionFailedResponse(
        JobStage jobStage, List<CheckPreconditionFailure> preconditionsFailures) {
      super(jobStage, JobStatus.FAILED);
      errorDetails.addAll(preconditionsFailures);
    }

    public String asText() {

      StringBuffer sb = new StringBuffer();
      sb.append(getStage() + KEY_VALUE_SEPARATOR + getStatus()).append(System.lineSeparator());
      sb.append(ERRORS_KEY + KEY_VALUE_SEPARATOR)
          .append(String.join(ERRORS_DELIMITER, Arrays.toString(errorDetails.toArray())))
          .append(System.lineSeparator());

      return sb.toString();
    }
  }
}
