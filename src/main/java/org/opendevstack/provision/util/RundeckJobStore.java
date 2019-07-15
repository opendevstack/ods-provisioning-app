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
import org.opendevstack.provision.model.rundeck.Job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Jobstore that will be created as bean to store rundeck Jobs centrally. Prevent unnecessary calls
 * to rundeck API
 *
 * @author Torsten Jaeschke
 */

public class RundeckJobStore {
    private Map<String, Job> jobs = new HashMap<>();

  public void addJob(Job job) {
        this.jobs.put(job.getId(), job);
    }

  public void addJobs(List<Job> jobs) {
    for (Job job : jobs) {
            this.jobs.put(job.getId(), job);
        }
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
  public Job getJob(String id) {
        return jobs.get(id);
    }

    /**
     * get a job list specified with their IDs
     * 
     * @param jobIds
     * @return
     */
  public List<Job> getJobs(List<String> jobIds) {
        ArrayList<Job> jobList = new ArrayList<>();
    for (String id : jobIds) {
            jobList.add(jobs.get(id));
        }
        return jobList;
    }

    @Override
  public String toString() {
    String jobsString = "";
    for (String job : jobs.keySet()) {
      jobsString = jobsString + " " + job;
    }
    return jobsString;
  }

  public int size() {
        return jobs.size();
    }

  public String getJobIdForJobName(String jobName) {
    Preconditions.checkNotNull(jobName, "Jobname cannot be null");

    for (Job job : jobs.values()) {
      if (job.getName().equalsIgnoreCase(jobName)) {
                return job.getId();
            }
        }
        return null;
    }

}
