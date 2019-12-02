package org.opendevstack.provision.config;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Quickstarter {

  private String name;
  private String url;
  private String desc;
  private QuickstarterType type = QuickstarterType.component;

  public Quickstarter() {}

  private Quickstarter(String name, String url, String desc) {
    this.name = name;
    this.url = url;
    this.desc = desc;
  }

  public static Quickstarter componentQuickstarter(String name, String url, String desc) {
    Quickstarter result = new Quickstarter(name, url, desc);
    result.type = QuickstarterType.component;
    return result;
  }

  public static Quickstarter projectQuickstarter(String name, String url, String desc) {
    Quickstarter result = new Quickstarter(name, url, desc);
    result.type = QuickstarterType.project;
    return result;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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

  public boolean isComponentQuickstarter() {
    return QuickstarterType.component.equals(this.getType());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("url", url)
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
