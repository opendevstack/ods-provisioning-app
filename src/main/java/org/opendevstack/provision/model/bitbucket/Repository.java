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

package org.opendevstack.provision.model.bitbucket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * { "name": "My repo", "scmId": "git", "forkable": true }
 *
 * @author Torsten Jaeschke
 */
@Generated(value = { "JSON-to-Pojo-Generator" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository
{

    private String name;
    private String scmId = "git";
    private boolean forkable = true;

    @JsonIgnoreProperties({ "adminGroup", "userGroup" })
    String adminGroup;
    String userGroup;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getScmId()
    {
        return scmId;
    }

    public void setScmId(String scmId)
    {
        this.scmId = scmId;
    }

    public boolean isForkable()
    {
        return forkable;
    }

    public void setForkable(boolean forkable)
    {
        this.forkable = forkable;
    }

    public void setAdminGroup(String adminGroup)
    {
        this.adminGroup = adminGroup;
    }

    public String getAdminGroup()
    {
        return this.adminGroup;
    }

    public void setUserGroup(String userGroup)
    {
        this.userGroup = userGroup;
    }

    public String getUserGroup()
    {
        return this.userGroup;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (forkable ? 1231 : 1237);
        result = prime * result
                + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((scmId == null) ? 0 : scmId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Repository other = (Repository) obj;
        if (forkable != other.forkable)
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (scmId == null)
        {
            if (other.scmId != null)
                return false;
        } else if (!scmId.equals(other.scmId))
            return false;
        return true;
    }

}
