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

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Generated(value = { "JSON-to-Pojo-Generator" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "holder", "permission" })
public class Permission
{

    @JsonProperty("holder")
    private Holder holder;
    @JsonProperty("permission")
    private String permission;

    @JsonProperty("holder")
    public Holder getHolder()
    {
        return holder;
    }

    @JsonProperty("holder")
    public void setHolder(Holder holder)
    {
        this.holder = holder;
    }

    @JsonProperty("permission")
    public String getPermission()
    {
        return permission;
    }

    @JsonProperty("permission")
    public void setPermission(String permission)
    {
        this.permission = permission;
    }

}
