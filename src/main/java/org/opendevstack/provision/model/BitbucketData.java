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

import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

import org.opendevstack.provision.model.bitbucket.Link;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * {"key":"SUBTA","id":1,"name":"Subtaskeditor","description":"Das ist ein
 * Testprojekt","public":false,"type":"NORMAL","links":{"self":[{"href":"http://192.168.56.31:7990/projects/SUBTA"}]}}
 *
 * @author Torsten Jaeschke
 */
@Generated(value = { "JSON-to-Pojo-Generator" })
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BitbucketData
{

    private String id;
    private String key;
    private String name;
    private String description;
    private String type;
    private Map<String, List<Link>> links;
    private String url;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Map<String, List<Link>> getLinks()
    {
        return links;
    }

    public void setLinks(Map<String, List<Link>> links)
    {
        this.links = links;
    }
}
