package org.opendevstack.provision.model.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class JobTest {

  private String odsGitRef = "production";

  @Test
  public void createJobOnlyWithNameAndRepo() {
    Job job = new Job("jobName", "gitRepoName.git", Optional.empty(), Optional.empty(), odsGitRef);
    assertEquals("jobName", job.getName());
    assertTrue(job.isEnabled());
    assertEquals(odsGitRef, job.getBranch());
    assertEquals("gitRepoName", job.getGitRepoName());
    assertEquals("jobName/Jenkinsfile", job.getJenkinsfilePath());
  }

  @Test
  public void createJobWithCustomBranch() {
    Job job =
        new Job(
            "jobName", "gitRepoName.git", Optional.of("customBranch"), Optional.empty(), odsGitRef);
    assertEquals("jobName", job.getName());
    assertTrue(job.isEnabled());
    assertEquals("customBranch", job.getBranch());
    assertEquals("gitRepoName", job.getGitRepoName());
    assertEquals("jobName/Jenkinsfile", job.getJenkinsfilePath());
  }

  @Test
  public void createJobWithCustomJenkinsfilePath() {
    Job job =
        new Job(
            "jobName",
            "gitRepoName.git",
            Optional.empty(),
            Optional.of("a/custom/path/Jenkinsfile"),
            odsGitRef);
    assertEquals("jobName", job.getName());
    assertTrue(job.isEnabled());
    assertEquals(odsGitRef, job.getBranch());
    assertEquals("gitRepoName", job.getGitRepoName());
    assertEquals("a/custom/path/Jenkinsfile", job.getJenkinsfilePath());
  }
}
