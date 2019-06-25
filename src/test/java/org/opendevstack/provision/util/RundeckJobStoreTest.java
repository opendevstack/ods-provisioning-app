/*
 * Copyright 2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.rundeck.Job;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class RundeckJobStoreTest
{

    RundeckJobStore jobStore;

    @Before
    public void setUp()
    {
        jobStore = new RundeckJobStore();
    }

    @Test
    public void addJob() throws Exception
    {
        Job job = generateTestJobs().get(0);
        jobStore.addJob(generateTestJobs().get(0));

        assertEquals(jobStore.getJob("1").getId(), job.getId());
    }

    @Test
    public void addJobs() throws Exception
    {
        List<Job> jobs = generateTestJobs();
        List<String> ids = Arrays.asList("1", "2");
        jobStore.addJobs(jobs);

        assertEquals(jobStore.getJobs(ids), jobs);
    }

    @Test
    public void removeJob() throws Exception
    {
        List<Job> jobs = generateTestJobs();
        jobStore.addJobs(jobs);
        jobStore.removeJob("1");

        assertNull(jobStore.getJob("1"));
    }

    @Test
    public void getJob() throws Exception
    {
        List<Job> jobs = generateTestJobs();
        jobStore.addJob(jobs.get(0));

        assertNotNull(jobStore.getJob("1"));
    }

    @Test
    public void getJobs() throws Exception
    {
        List<Job> jobs = generateTestJobs();
        List<String> ids = Arrays.asList("1", "2");
        jobStore.addJobs(jobs);

        assertTrue(jobStore.getJobs(ids).size() > 0);
    }

    private List<Job> generateTestJobs()
    {
        List<Job> jobs = new ArrayList<>();
        Job j1 = new Job();
        j1.setName("j1");
        j1.setId("1");
        Job j2 = new Job();
        j2.setName("j2");
        j2.setId("2");
        jobs.add(j1);
        jobs.add(j2);
        return jobs;
    }
}
