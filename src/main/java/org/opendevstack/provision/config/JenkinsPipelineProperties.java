package org.opendevstack.provision.config;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "jenkinspipeline")
@Configuration
public class JenkinsPipelineProperties {

  private Map<String, Quickstarter> quickstarter;

  @PostConstruct
  public void setNameInAllQuickstarters() {
    quickstarter.entrySet().forEach(entry -> entry.getValue().setName(entry.getKey()));
  }

  public Map<String, Quickstarter> getQuickstarter() {
    return quickstarter;
  }

  public void setQuickstarter(Map<String, Quickstarter> quickstarter) {
    this.quickstarter = quickstarter;
  }

  public Quickstarter getCreateProjectQuickstarter() {
    String name = "create-projects";
    return quickstarter.values().stream()
        .filter(q -> q.getName().equals(name))
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Invalid confiugration. Quickstarter with name "
                        + name
                        + " is not found in configuration"));
  }
}
