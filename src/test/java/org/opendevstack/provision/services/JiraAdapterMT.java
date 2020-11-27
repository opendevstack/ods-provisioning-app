package org.opendevstack.provision.services;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opendevstack.provision.adapter.IServiceAdapter.LIFECYCLE_STAGE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.LocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"oauth2", "oauth2private"})
@Disabled("Only exists for manual execution")
public class JiraAdapterMT {

  @Autowired private JiraAdapter jiraAdapter;

  @Autowired private LocalStorage localStorage;

  @Test
  public void deletesProjects() {
    // take care that you use an existing jira project that can be deleted. Since this is an
    // real integration test, it will really delete the project.
    OpenProjectData project = localStorage.getProject("TEST34");
    jiraAdapter.cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, project);
  }
}
