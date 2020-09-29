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
import javax.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

/** @author Sebastian Titakis */
@Validated
public class JiraProjectTemplate {

  @NotNull private String name;

  @NotNull @Positive private Integer permissionSchemeId;

  @NotNull private JiraProjectTemplateRoleMapping roleMapping;

  public JiraProjectTemplate() {}

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JiraProjectTemplate that = (JiraProjectTemplate) o;

    return name != null ? name.equals(that.name) : that.name == null;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getPermissionSchemeId() {
    return permissionSchemeId;
  }

  public void setPermissionSchemeId(Integer permissionSchemeId) {
    this.permissionSchemeId = permissionSchemeId;
  }

  public JiraProjectTemplateRoleMapping getRoleMapping() {
    return roleMapping;
  }

  public void setRoleMapping(JiraProjectTemplateRoleMapping roleMapping) {
    this.roleMapping = roleMapping;
  }
}
