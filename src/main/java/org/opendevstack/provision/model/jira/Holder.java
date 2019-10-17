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

package org.opendevstack.provision.model.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.annotation.Generated;

@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "parameter"})
public class Holder {

  @JsonProperty("type")
  private String type;

  @JsonProperty("parameter")
  private String parameter;

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("parameter")
  public String getParameter() {
    return parameter;
  }

  @JsonProperty("parameter")
  public void setParameter(String parameter) {
    this.parameter = parameter;
  }
}
