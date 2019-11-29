package org.opendevstack.provision.model.webhookproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
  private String kind;
  private String namespace;
  private String name;

  public String getKind() {
    return kind;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void setName(String name) {
    this.name = name;
  }
}
