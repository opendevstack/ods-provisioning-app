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
package org.opendevstack.provision.adapter.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author Sebastian Titakis */
public class CreateProjectPreconditionException extends Exception {

  private final String adapterName;

  private final String projectKey;

  private List failures = new ArrayList();

  public CreateProjectPreconditionException(
      String adapterName, String projectKey) {
    this(adapterName, projectKey, null);
  }

  public CreateProjectPreconditionException(
      String adapterName, String projectKey, Exception cause) {
    super(
        CreateProjectPreconditionException.buildMessage(adapterName, projectKey, cause.getMessage()),
        cause);
    this.adapterName = adapterName;
    this.projectKey = projectKey;
  }

  public String getAdapterName() {
    return adapterName;
  }

  public String getProjectKey() {
    return projectKey;
  }

  private static String buildMessage(String adapterName, String projectKey, String failureMessage) {
    return String.format(
        "'%s' found a precondition failure for project '%s': %s",
        adapterName, projectKey, failureMessage);
  }

  public void addFailure(String failureMessage) {
    failures.add(failureMessage);
  }

  public List getFailures() {
    return Collections.unmodifiableList(failures);
  }
}
