package org.opendevstack.provision.config;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "jenkinspipeline")
@Configuration
public class JenkinsPipelineProperties {

  private final Map<String, Quickstarter> quickstarter = new HashMap<>();
  public final String QUICKSTARTER_CREATE_PROJECTS = "create-projects";
  public final String QUICKSTARTER_DELETE_PROJECTS = "delete-projects";

  @PostConstruct
  public void setNameInAllQuickstarters() {
    quickstarter.entrySet().forEach(entry -> entry.getValue().setName(entry.getKey()));
  }

  public Map<String, Quickstarter> getQuickstarter() {
    return quickstarter;
  }

  public void setQuickstarter(Map<String, Quickstarter> quickstarter) {
    this.quickstarter.putAll(quickstarter);
  }

  public void addQuickstarter(Quickstarter quickstarter) {
    this.quickstarter.put(quickstarter.getName(), quickstarter);
  }

  public Quickstarter getCreateProjectQuickstarter() {
    return getQuickstarter(QUICKSTARTER_CREATE_PROJECTS);
  }

  public Quickstarter getDeleteProjectQuickstarter() {
    return getQuickstarter(QUICKSTARTER_DELETE_PROJECTS);
  }

  public boolean isCreateOrDeleteProjectJob(String jobId) {
    return QUICKSTARTER_CREATE_PROJECTS.equals(jobId) || QUICKSTARTER_DELETE_PROJECTS.equals(jobId);
  }

  private Quickstarter getQuickstarter(String name) {
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
