package org.opendevstack.provision.model.jenkins;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JobTest {

  @Test
  public void createJobFromUrlWithBranchSpecification() {
    Job job =
        new Job(
            "jobName", "gitParentProject/gitRepoName.git#branch/path-to/Jenkinsfile", "legacyCt");
    checkCommonJobParameters(job);
    Assertions.assertThat(job.getBranch()).isEqualTo("branch");
  }

  @Test
  public void createJobFromUrlWithoutBranchSpecification() {
    Job job =
        new Job("jobName", "gitParentProject/gitRepoName.git/path-to/Jenkinsfile", "legacyCt");
    checkCommonJobParameters(job);
    Assertions.assertThat(job.getBranch()).isEqualTo("master");
  }

  private void checkCommonJobParameters(Job job) {
    Assertions.assertThat(job.getId()).isEqualTo("jobName");
    Assertions.assertThat(job.isEnabled()).isTrue();
    Assertions.assertThat(job.getName()).isEqualTo("jobName");
    Assertions.assertThat(job.getGitParentProject()).isEqualTo("gitParentProject");
    Assertions.assertThat(job.getGitRepoName()).isEqualTo("gitRepoName");
    Assertions.assertThat(job.getJenkinsfilePath()).isEqualTo("path-to/Jenkinsfile");
  }
}
