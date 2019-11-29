package org.opendevstack.provision.config;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "jenkinspipeline")
@Configuration
public class JenkinsPipelineProperties {

  private Map<String, Quickstarter> quickstarter;

  public Map<String, Quickstarter> getQuickstarter() {
    return quickstarter;
  }

  @PostConstruct
  public void setNameInAllQuickstarters() {
    quickstarter.entrySet().forEach(entry -> entry.getValue().setName(entry.getKey()));
  }

  public void setQuickstarter(Map<String, Quickstarter> quickstarter) {
    this.quickstarter = quickstarter;
  }
}
