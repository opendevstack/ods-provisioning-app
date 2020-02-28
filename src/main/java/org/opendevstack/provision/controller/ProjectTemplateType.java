package org.opendevstack.provision.controller;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectTemplateType {

  private final String key;
  private final String name;

  public ProjectTemplateType(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectTemplateType that = (ProjectTemplateType) o;

    return key != null ? key.equals(that.key) : that.key == null;
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  public static List<ProjectTemplateType> createList(List<String> projectTemplateKeyNames) {
    return projectTemplateKeyNames.stream()
        .map(name -> new ProjectTemplateType(name, name))
        .collect(Collectors.toList());
  }

  public static String createProjectTemplateKeysAsString(List<String> keys) {
    return "["
        + String.join(",", keys.stream().map(s -> "\\\"" + s + "\\\"").collect(Collectors.toList()))
        + "]";
  }
}
