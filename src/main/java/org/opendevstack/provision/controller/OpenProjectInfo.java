package org.opendevstack.provision.controller;

import org.opendevstack.provision.model.OpenProjectData;

/** Implement a "view" of OpenProjectData */
public class OpenProjectInfo {

  private OpenProjectData project;

  public OpenProjectInfo(OpenProjectData project) {
    this.project = project;
  }

  public String getProjectKey() {
    return project.projectKey;
  }

  public String getProjectName() {
    return project.projectName;
  }

  public boolean hasBugtrackerSpace() {
    return project.bugtrackerSpace;
  }

  public boolean hasPlatformRuntime() {
    return project.platformRuntime;
  }

  public String getDescription() {
    return project.description;
  }

  public String getCDUser() {
    return project.cdUser;
  }

  public String getCollaborationSpaceUrl() {
    return project.collaborationSpaceUrl;
  }

  public String getPlatformBuildEngineUrl() {
    return project.platformBuildEngineUrl;
  }

  public String getPlatformCdEnvironmentUrl() {
    return project.platformCdEnvironmentUrl;
  }

  public String getPlatformDevEnvironmentUrl() {
    return project.platformDevEnvironmentUrl;
  }

  public String getPlatformTestEnvironmentUrl() {
    return project.platformTestEnvironmentUrl;
  }

  public String getBugtrackerUrl() {
    return project.bugtrackerUrl;
  }

  public String getSCMVCSUrl() {
    return project.scmvcsUrl;
  }
}
