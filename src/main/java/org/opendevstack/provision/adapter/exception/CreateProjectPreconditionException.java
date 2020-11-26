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

public class CreateProjectPreconditionException extends Exception {

  private final String adapterName;

  private final String projectKey;

  public CreateProjectPreconditionException(
      String adapterName, String projectKey, AdapterException cause) {
    super(CreateProjectPreconditionException.buildMessage(adapterName, projectKey, cause), cause);
    this.adapterName = adapterName;
    this.projectKey = projectKey;
  }

  public CreateProjectPreconditionException(String adapterName, String projectKey, String cause) {
    super(cause);
    this.adapterName = adapterName;
    this.projectKey = projectKey;
  }

  public String getAdapterName() {
    return adapterName;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public static String buildMessage(String adapterName, String projectKey, Throwable cause) {
    return String.format(
        "%s was thrown in adapter '%s' while executing check preconditions for project '%s'. [message=%s]",
        cause.getClass(), adapterName, projectKey, cause.getMessage());
  }
}
