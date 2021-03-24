package org.opendevstack.provision.model.webhookproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
  private String phase;
  private Config config;

  public String getPhase() {
    return phase;
  }

  public Config getConfig() {
    return config;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public void setConfig(Config configObject) {
    this.config = configObject;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("phase", phase)
        .append("config", config)
        .toString();
  }
}
