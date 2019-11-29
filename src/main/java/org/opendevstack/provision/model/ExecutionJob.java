package org.opendevstack.provision.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ExecutionJob {

  private String name;
  private String url;

  public ExecutionJob() {}

  /**
   * Needed for data without job name. In this case, the value of url is written to name
   *
   * @param url
   */
  public ExecutionJob(String url) {
    this.name = url;
    this.url = url;
  }

  public ExecutionJob(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("url", url)
        .append("name", name)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ExecutionJob)) {
      return false;
    }

    ExecutionJob that = (ExecutionJob) o;

    return new EqualsBuilder().append(name, that.name).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name).toHashCode();
  }
}
