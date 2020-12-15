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
public class BitbucketAdapterMT {

  @Autowired private BitbucketAdapter bitbucketAdapter;

  @Autowired private LocalStorage localStorage;

  @Test
  public void deletesProjects() {
    OpenProjectData project = localStorage.getProject("TEST6");
    bitbucketAdapter.cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, project);
  }
}
