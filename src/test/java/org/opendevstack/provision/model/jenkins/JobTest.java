package org.opendevstack.provision.model.jenkins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;

public class JobTest {

  private String odsGitRef = "production";

  @Test
  public void createJobOnlyWithNameAndRepo() {
    Job job = new Job("jobName", "gitRepoName.git", Optional.empty(), Optional.empty(), odsGitRef);
    assertThat(job.getName()).isEqualTo("jobName");
    assertThat(job.isEnabled()).isTrue();
    assertThat(job.getBranch()).isEqualTo(odsGitRef);
    assertThat(job.getGitRepoName()).isEqualTo("gitRepoName");
    assertThat(job.getJenkinsfilePath()).isEqualTo("jobName/Jenkinsfile");
  }

  @Test
  public void createJobWithCustomBranch() {
    Job job =
        new Job(
            "jobName", "gitRepoName.git", Optional.of("customBranch"), Optional.empty(), odsGitRef);
    assertThat(job.getName()).isEqualTo("jobName");
    assertThat(job.isEnabled()).isTrue();
    assertThat(job.getBranch()).isEqualTo("customBranch");
    assertThat(job.getGitRepoName()).isEqualTo("gitRepoName");
    assertThat(job.getJenkinsfilePath()).isEqualTo("jobName/Jenkinsfile");
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
    assertThat(job.getName()).isEqualTo("jobName");
    assertThat(job.isEnabled()).isTrue();
    assertThat(job.getBranch()).isEqualTo(odsGitRef);
    assertThat(job.getGitRepoName()).isEqualTo("gitRepoName");
    assertThat(job.getJenkinsfilePath()).isEqualTo("a/custom/path/Jenkinsfile");
  }
}
