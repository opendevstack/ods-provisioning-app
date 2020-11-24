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

import javax.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;


@Validated
public class JiraProjectTemplateRoleMapping {

  @NotNull private String projectRoleForAdminGroup;

  @NotNull private String projectRoleForUserGroup;

  @NotNull private String projectRoleForReadonlyGroup;

  public String getProjectRoleForAdminGroup() {
    return projectRoleForAdminGroup;
  }

  public void setProjectRoleForAdminGroup(String projectRoleForAdminGroup) {
    this.projectRoleForAdminGroup = projectRoleForAdminGroup;
  }

  public String getProjectRoleForUserGroup() {
    return projectRoleForUserGroup;
  }

  public void setProjectRoleForUserGroup(String projectRoleForUserGroup) {
    this.projectRoleForUserGroup = projectRoleForUserGroup;
  }

  public String getProjectRoleForReadonlyGroup() {
    return projectRoleForReadonlyGroup;
  }

  public void setProjectRoleForReadonlyGroup(String projectRoleForReadonlyGroup) {
    this.projectRoleForReadonlyGroup = projectRoleForReadonlyGroup;
  }
}
