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

package org.opendevstack.provision.services;

import java.util.Map;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.storage.IStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to interact with the underlying storage system to liast the project history
 *
 * @author Torsten Jaeschke
 */

@Service
public class StorageAdapter {

  @Autowired
  IStorage storage;

  private static final Logger logger = LoggerFactory.getLogger(StorageAdapter.class);

  public Map<String, ProjectData> listProjectHistory() {
    return storage.listProjectHistory();
  }

  public ProjectData getProject(String id) {
    return null;
  }

  public AboutChangesData listAboutChangesData() {
    return storage.listAboutChangesData();
  }

}
