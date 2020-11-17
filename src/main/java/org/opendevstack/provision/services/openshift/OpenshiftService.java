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
package org.opendevstack.provision.services.openshift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendevstack.provision.adapter.exception.AdapterException;
import org.opendevstack.provision.adapter.exception.CreateProjectPreconditionException;
import org.opendevstack.provision.controller.CheckPreconditionFailure;
import org.opendevstack.provision.model.OpenProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/** @author Sebastian Titakis */
public class OpenshiftService {

  private static final Logger logger = LoggerFactory.getLogger(OpenshiftService.class);

  public static final String SERVICE_NAME = "openshiftService";

  private OpenshiftClient openshiftClient;

  public OpenshiftService(OpenshiftClient openshiftClient) {
    this.openshiftClient = openshiftClient;
  }

  public List<CheckPreconditionFailure> checkCreateProjectPreconditions(OpenProjectData newProject)
      throws CreateProjectPreconditionException {

    try {
      Assert.notNull(newProject, "Parameter 'newProject' is null!");
      Assert.notNull(
          newProject.projectKey, "Properties 'projectKey' of parameter 'newProject' is null!");

      logger.info("checking create project preconditions for project '{}'!", newProject.projectKey);

      List<CheckPreconditionFailure> preconditionFailures =
          createProjectKeyExistsCheck(newProject.projectKey);

      logger.info(
          "done with check create project preconditions for project '{}'!", newProject.projectKey);

      return preconditionFailures;

    } catch (AdapterException e) {
      throw new CreateProjectPreconditionException(SERVICE_NAME, newProject.projectKey, e);
    } catch (Exception e) {
      String message =
          String.format(
              "Unexpected error when checking precondition for creation of project '%s'",
              newProject.projectKey);
      logger.error(message, e);
      throw new CreateProjectPreconditionException(SERVICE_NAME, newProject.projectKey, message);
    }
  }

  public List<CheckPreconditionFailure> createProjectKeyExistsCheck(String projectKey) {

    try {
      logger.info("Checking if ODS project '{}-*'  exists in openshift!", projectKey);

      List<CheckPreconditionFailure> preconditionFailures = new ArrayList<>();

      Set<String> projects = openshiftClient.projects();
      List<String> existingProjects =
          projects.stream()
              .filter(s -> s.toLowerCase().startsWith(projectKey.toLowerCase() + "-"))
              .collect(Collectors.toList());

      if (existingProjects.size() > 0) {
        String message =
            String.format(
                "Project name (namespace) with prefix '%s' already exists in '%s'! [existingProjects=%s]",
                projectKey, SERVICE_NAME, Arrays.asList(existingProjects.toArray()));
        preconditionFailures.add(CheckPreconditionFailure.getProjectExistsInstance(message));
      }

      return preconditionFailures;

    } catch (Exception ex) {
      throw new AdapterException(ex);
    }
  }
}
