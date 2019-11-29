package org.opendevstack.provision.model.webhookproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Annotations {

  @JsonProperty("openshift.io/build-config.name")
  private String buildConfigName;

  @JsonProperty("openshift.io/build.number")
  private String buildNumber;

  public String getBuildConfigName() {
    return buildConfigName;
  }

  public void setBuildConfigName(String buildConfigName) {
    this.buildConfigName = buildConfigName;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
  }
}
