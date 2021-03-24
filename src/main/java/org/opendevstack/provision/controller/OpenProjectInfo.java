package org.opendevstack.provision.controller;

import org.opendevstack.provision.model.OpenProjectData;

/** Implement a "view" of OpenProjectData */
public class OpenProjectInfo {

  private OpenProjectData project;

  public OpenProjectInfo(OpenProjectData project) {
    this.project = project;
  }

  public String getProjectKey() {
    return project.getProjectKey();
  }

  public String getProjectName() {
    return project.getProjectName();
  }

  public String getDescription() {
    return project.getDescription();
  }

  public String getCDUser() {
    return project.getCdUser();
  }

  public String getCollaborationSpaceUrl() {
    return project.getCollaborationSpaceUrl();
  }

  public String getPlatformCdEnvironmentUrl() {
    return project.getPlatformCdEnvironmentUrl();
  }

  public String getSCMVCSUrl() {
    return project.getScmvcsUrl();
  }
}
