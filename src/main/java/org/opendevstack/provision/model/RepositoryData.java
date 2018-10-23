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

package org.opendevstack.provision.model;

import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

import org.opendevstack.provision.model.bitbucket.Link;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * { "slug": "testrepo", "id": 1, "name": "testrepo", "scmId": "git", "state": "AVAILABLE",
 * "statusMessage": "Available", "forkable": true, "project": { "key": "DRIMM", "id": 4, "name":
 * "drimmes", "description": "dsdfdsfsdfsdf", "public": false, "type": "NORMAL", "links": { "self":
 * [ { "href": "http://192.168.56.31:7990/projects/DRIMM" } ] } }, "public": false, "links": {
 * "clone": [ { "href": "ssh://git@192.168.56.31:7999/drimm/testrepo.git", "name": "ssh" }, {
 * "href": "http://admin@192.168.56.31:7990/scm/drimm/testrepo.git", "name": "http" } ], "self": [ {
 * "href": "http://192.168.56.31:7990/projects/DRIMM/repos/testrepo/browse" } ] } }
 * 
 * @author Torsten Jaeschke
 */
@Generated(value = {"JSON-to-Pojo-Generator"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryData {
  private String slug;
  private String id;
  private String name;
  private Map<String, List<Link>> links;

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, List<Link>> getLinks() {
    return links;
  }

  public void setLinks(Map<String, List<Link>> links) {
    this.links = links;
  }
}
