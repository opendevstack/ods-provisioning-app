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

package org.opendevstack.provision.controller;

import java.io.IOException;
import java.util.List;

import org.opendevstack.provision.adapter.IJobExecutionAdapter;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.model.rundeck.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restcontroller to communicate with the Rundeck API and handle the
 * quickstarters and related jobs
 *
 * @author Torsten Jaeschke
 */
@RestController
@RequestMapping(value = "api/v1/quickstarter")
public class QuickstarterApiController
{

    @Autowired
    private IJobExecutionAdapter rundeckAdapter;

    /**
     * Call to get the available quickstarters
     *
     * @return JSON string
     */
    @RequestMapping(produces = {
            "application/json" }, method = RequestMethod.GET)
    public ResponseEntity<List<Job>> getTechTemplates()
    {
        return ResponseEntity.ok()
                .body(rundeckAdapter.getQuickstarters());
    }

    @RequestMapping(value = "/provision", produces = {
            "application/json" }, method = RequestMethod.POST)
    public ResponseEntity<List<ExecutionsData>> runJobs(
            @RequestBody ProjectData project) throws IOException
    {
        List<ExecutionsData> executions = rundeckAdapter
                .provisionComponentsBasedOnQuickstarters(project);
        return ResponseEntity.ok(executions);
    }
}
