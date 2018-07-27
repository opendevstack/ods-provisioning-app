/*
 * Copyright 2018 the original author or authors.
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

package org.opendevstack.provision.model.confluence;

/**
 * @author Torsten Jaeschke
 */
public class Space {
  private String spaceKey = null;
  private String name = null;
  private String description = null;
  private String spaceBlueprintId = null;
  private Context context = null;

  public String getSpaceKey() {
    return spaceKey;
  }

  public void setSpaceKey(String spaceKey) {
    this.spaceKey = spaceKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSpaceBlueprintId() {
    return spaceBlueprintId;
  }

  public void setSpaceBlueprintId(String spaceBlueprintId) {
    this.spaceBlueprintId = spaceBlueprintId;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  /*
   * {
   * 
   * "spaceKey":"FDSSSDF", "name":"fdsssdf", "description":"",
   * "spaceBlueprintId":"557f8a6a-667c-4eee-ac39-519396805199", "context": {
   * "jira-server":"06509280-5441-3f2e-b5d1-60fcc24a85aa", "jira-project":"10200", "name":"fdsssdf",
   * "spaceKey":"FDSSSDF", "description":"", "noPageTitlePrefix":"true", "atl_token":"undefined",
   * "jira-server-id":"06509280-5441-3f2e-b5d1-60fcc24a85aa", "project-key":"FDSSS",
   * "project-name":"fdsssdf", "ContentPageTitle":"fdsssdf" } }
   */


}
