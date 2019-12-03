package org.opendevstack.provision.util;

import org.opendevstack.provision.model.webhookproxy.Annotations;
import org.opendevstack.provision.model.webhookproxy.CreateProjectResponse;
import org.opendevstack.provision.model.webhookproxy.Metadata;

public class CreateProjectResponseUtil {

  public static CreateProjectResponse buildDummyCreateProjectResponse(
      String namespace, String buildConfigName, int buildNumber) {
    CreateProjectResponse response = new CreateProjectResponse();
    Metadata metadata = new Metadata();
    metadata.setNamespace(namespace);
    Annotations annotations = new Annotations();
    annotations.setBuildNumber(String.valueOf(buildNumber));
    annotations.setBuildConfigName(buildConfigName);
    metadata.setAnnotations(annotations);
    response.setMetadata(metadata);
    return response;
  }
}
