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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Generated;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * error: {"error":true,"apiversion":19,"errorCode":"api.error.job.options-invalid","message":"Job
 * options were not valid: Option 'git_url_https' is required."}
 *
 * @author Torsten Jaeschke
 */
@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionsData {
  Integer id;
  String status;
  String errorCode;
  String message;
  boolean error;
  String jobName;
  String permalink;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setPermalink(String permalink) {
    this.permalink = permalink;
  }

  public String getPermalink() {
    return this.permalink;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return this.errorCode;
  }

  public void setError(boolean error) {
    this.error = error;
  }

  public boolean getError() {
    return error;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("id", id)
        .append("status", status)
        .append("errorCode", errorCode)
        .append("message", message)
        .append("error", error)
        .append("jobName", jobName)
        .append("permalink", permalink)
        .toString();
  }
}
