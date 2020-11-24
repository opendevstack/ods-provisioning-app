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

package org.opendevstack.provision.adapter;

import java.util.List;
import java.util.Map;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.controller.ProjectApiController;
import org.opendevstack.provision.model.OpenProjectData;

/**
 * Base interface for all service adapters
 */
public interface IServiceAdapter {

  /** Stage the adapters are in during project creation & update */
  enum LIFECYCLE_STAGE {
    INITIAL_CREATION,
    QUICKSTARTER_PROVISION
  }
  /** Enum used for cleanup to define any leftovers after cleanup */
  enum CLEANUP_LEFTOVER_COMPONENTS {
    COLLABORATION_SPACE,
    BUGTRACKER_PROJECT,
    SCM_PROJECT,
    SCM_REPO,
    QUICKSTARTER,
    PLTF_PROJECT,
    PROJECT_DB
  }
  /** Project template key enum */
  enum PROJECT_TEMPLATE {
    TEMPLATE_KEY,
    TEMPLATE_TYPE_KEY
  }

  /**
   * Return a list of project per adapter
   *
   * @param filter a filter (e.g. key), can be null
   * @return a map with project key and name, never null, but potentially empty
   */
  Map<String, String> getProjects(String filter);

  /**
   * Get the adapter's used rest / api URI
   *
   * @return the URI
   */
  String getAdapterApiUri();

  /**
   * Called by {@link ProjectApiController} in case of error, and need to cleanup already created
   * resources. As the API controller has no idea what the {@link IServiceAdapter} implementations
   * do, the best we can do is to delegate the work for cleanup there as well.
   *
   * @param stage the stage the adapter is currently in
   * @param project the project
   * @return a map with component amounts that could not be cleaned up
   */
  Map<CLEANUP_LEFTOVER_COMPONENTS, Integer> cleanup(LIFECYCLE_STAGE stage, OpenProjectData project);

  /**
   * @param newProject
   * @return
   */
  List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException;
}
