package org.opendevstack.provision.model.webhookproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Labels {

  private String buildconfig;

  @JsonProperty("openshift.io/build-config.name")
  private String buildConfigName;

  public String getBuildconfig() {
    return buildconfig;
  }

  public void setBuildconfig(String buildconfig) {
    this.buildconfig = buildconfig;
  }

  public String getBuildConfigName() {
    return buildConfigName;
  }

  public void setBuildConfigName(String buildConfigName) {
    this.buildConfigName = buildConfigName;
  }
}
