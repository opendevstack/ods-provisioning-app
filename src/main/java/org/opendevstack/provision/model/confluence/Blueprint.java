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

package org.opendevstack.provision.model.confluence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Generated;

@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Blueprint {
  private String blueprintModuleCompleteKey;
  private String contentBlueprintId;

  public String getBlueprintModuleCompleteKey() {
    return blueprintModuleCompleteKey;
  }

  public void setBlueprintModuleCompleteKey(String blueprintModuleCompleteKey) {
    this.blueprintModuleCompleteKey = blueprintModuleCompleteKey;
  }

  public String getContentBlueprintId() {
    return contentBlueprintId;
  }

  public void setContentBlueprintId(String contentBlueprintId) {
    this.contentBlueprintId = contentBlueprintId;
  }
}
