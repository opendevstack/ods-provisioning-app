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

package org.opendevstack.provision.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.opendevstack.provision.model.ProjectData;
import org.opendevstack.provision.storage.LocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class StorageAdapterTest
{

    @Mock
    LocalStorage storage;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    StorageAdapter adapter;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void listProjectHistoryNoAuth() throws Exception
    {
        Mockito.when(storage.listProjectHistory())
                .thenReturn(new HashMap<>());
        adapter.setStorage(storage);

        assertTrue(adapter.listProjectHistory().isEmpty());
    }

    @Test
    public void listProjectHistoryWithAuth() throws Exception
    {
        try
        {
            // open project
            ProjectData data = new ProjectData();
            data.name = "testproject";
            data.key = "testpprojectKey";
            data.adminGroup = "testgroup";

            // case sensitive right group
            ProjectData dataProtected = new ProjectData();
            dataProtected.name = "testprojectProtected";
            dataProtected.key = "testprojectProtected";
            dataProtected.adminGroup = "testgroup";
            dataProtected.createpermissionset = true;

            // wrong group
            ProjectData dataProtectedWrong = new ProjectData();
            dataProtectedWrong.name = "testprojectProtectedW";
            dataProtectedWrong.key = "testprojectProtectedW";
            dataProtectedWrong.adminGroup = "testgroupW";
            dataProtectedWrong.createpermissionset = true;

            // group upper lower case
            ProjectData dataProtectedCase = new ProjectData();
            dataProtectedCase.name = "testprojectProtectedC";
            dataProtectedCase.key = "testprojectProtectedC";
            dataProtectedCase.adminGroup = "testGroup";
            dataProtectedCase.createpermissionset = true;

            Map<String, ProjectData> projects = new HashMap<>();
            projects.put(data.key, data);
            projects.put(dataProtected.key, dataProtected);
            projects.put(dataProtectedWrong.key, dataProtectedWrong);
            projects.put(dataProtectedCase.key, dataProtectedCase);

            Mockito.when(storage.listProjectHistory())
                    .thenReturn(projects);
            adapter.setStorage(storage);

            SecurityContextHolder.getContext()
                    .setAuthentication(new TestAuthentication());

            Map<String, ProjectData> testresult = adapter
                    .listProjectHistory();
            assertEquals(3, testresult.size());
            assertFalse(
                    testresult.containsKey(dataProtectedWrong.key));
        } finally
        {
            SecurityContextHolder.clearContext();
        }
    }

}
