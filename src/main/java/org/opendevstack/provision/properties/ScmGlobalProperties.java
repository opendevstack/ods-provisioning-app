package org.opendevstack.provision.properties;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("scm.global")
public class ScmGlobalProperties {

  private Map<String, List<String>> readableRepos;

  public Map<String, List<String>> getReadableRepos() {
    return readableRepos;
  }

  public void setReadableRepos(
      Map<String, List<String>> readableRepos) {
    this.readableRepos = readableRepos;
  }
}
