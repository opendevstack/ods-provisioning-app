/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendevstack.provision.util;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendevstack.provision.model.rundeck.Job;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Jobstore that will be created as bean to store rundeck Jobs centrally. Prevents unnecessary calls
 * to rundeck API.
 *
 * @author Torsten Jaeschke
 * @author Stefan Lack
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RundeckJobStore {
  private Map<String, Job> jobs = new HashMap<>();

  public void addJob(Job job) {
    this.jobs.put(job.getId(), job);
  }

  public void addJobs(List<Job> jobs) {
    jobs.forEach(this::addJob);
  }

  /**
   * remove a specified job from the store
   *
   * @param id
   */
  public void removeJob(String id) {
    this.jobs.remove(id);
  }

  /**
   * retrieve a specified job
   *
   * @param id
   * @return
   */
  public Job getJobById(String id) {
    return jobs.get(id);
  }

  /**
   * get a job list specified with their IDs
   *
   * @param jobIds
   * @return
   */
  public List<Job> getJobs(List<String> jobIds) {
    return jobIds.stream().map(jobs::get).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return String.join(" ", jobs.keySet());
  }

  public int size() {
    return jobs.size();
  }

  public Job getJobByNameOrId(String jobNameOrId) throws IOException {
    Job job = getJobById(jobNameOrId);

    if (job == null) {
      String jobByName = getJobIdForJobName(jobNameOrId);
      job = getJobById(jobByName);
    }

    if (job == null) {
      throw new IOException(
          String.format(
              "Cannot find job with name or id: %s! Available jobs: %s",
              jobNameOrId,
              jobs.values().stream()
                  .map(j -> j.toFormattedString())
                  .collect(Collectors.joining(","))));
    }
    return job;
  }

  public String getJobIdForJobName(String jobName) {
    Preconditions.checkNotNull(jobName, "Jobname cannot be null");

    String result = jobs.values().stream()
        .filter(job -> job.getName().equalsIgnoreCase(jobName))
        .findFirst()
        .map(j -> j.getId())
        .orElseGet(() -> null);
    return result;
  }

  public List<Job> getJobsByGroup(String group) {
    List<Job> result = jobs.values().stream().filter(groupEqual(group))
        .collect(Collectors.toList());
    return result;
  }

  public boolean hasJobWithGroup(String group) {
    boolean result = jobs.values().stream().anyMatch(groupEqual(group));
    return result;
  }

  public Predicate<Job> groupEqual(String group) {
    return candidate -> group.equals(candidate.getGroup());
  }
}
