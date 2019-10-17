package org.opendevstack.provision.services;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.adapter.IServiceAdapter.LIFECYCLE_STAGE;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.storage.LocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringBoot.class)
@ActiveProfiles("oauth2,oauth2private")
@Ignore("Only exists for manual execution")
public class BitbucketAdapterMT {

  @Autowired private BitbucketAdapter bitbucketAdapter;

  @Autowired private LocalStorage localStorage;

  @Test
  public void deletesProjects() {
    OpenProjectData project = localStorage.getProject("TEST6");
    bitbucketAdapter.cleanup(LIFECYCLE_STAGE.INITIAL_CREATION, project);
  }
}
