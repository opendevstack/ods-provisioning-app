package org.opendevstack.provision.config;

import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Quickstarter {

  private String name;
  private String repo;
  private String desc;
  private Optional<String> branch = Optional.empty();
  private Optional<String> jenkinsfile = Optional.empty();
  private QuickstarterType type = QuickstarterType.component;

  public Quickstarter() {}

  private Quickstarter(
      String name,
      String repo,
      String desc,
      Optional<String> branch,
      Optional<String> jenkinsfile) {
    this.name = name;
    this.repo = repo;
    this.desc = desc;
    this.branch = branch;
    this.jenkinsfile = jenkinsfile;
  }

  public static Quickstarter componentQuickstarter(
      String name, String url, String desc, Optional<String> branch, Optional<String> jenkinsfile) {
    Quickstarter result = new Quickstarter(name, url, desc, branch, jenkinsfile);
    result.type = QuickstarterType.component;
    return result;
  }

  public static Quickstarter adminjobQuickstarter(
      String name,
      String repo,
      String desc,
      Optional<String> branch,
      Optional<String> jenkinsfile) {
    Quickstarter result = new Quickstarter(name, repo, desc, branch, jenkinsfile);
    result.type = QuickstarterType.adminjob;
    return result;
  }

  public String getRepo() {
    return repo;
  }

  public void setRepo(String url) {
    this.repo = url;
  }

  public String getDesc() {
    return desc;
  }

  @SuppressWarnings("unused")
  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public QuickstarterType getType() {
    return type;
  }

  public void setType(QuickstarterType type) {
    this.type = type;
  }

  public Optional<String> getBranch() {
    return branch;
  }

  public void setBranch(Optional<String> branch) {
    this.branch = branch;
  }

  public Optional<String> getJenkinsfile() {
    return jenkinsfile;
  }

  public void setJenkinsfile(Optional<String> jenkinsfile) {
    this.jenkinsfile = jenkinsfile;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("repo", repo)
        .append("desc", desc)
        .append("type", type)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Quickstarter)) {
      return false;
    }

    Quickstarter that = (Quickstarter) o;

    return new EqualsBuilder().append(name, that.name).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name).toHashCode();
  }
}
