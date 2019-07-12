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

package org.opendevstack.provision.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ExecutionsData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.BitbucketProjectData;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.RepositoryData;
import org.opendevstack.provision.model.confluence.Blueprint;
import org.opendevstack.provision.model.confluence.JiraServer;
import org.opendevstack.provision.model.confluence.Space;
import org.opendevstack.provision.model.confluence.SpaceData;
import org.opendevstack.provision.model.jira.FullJiraProject;
import org.opendevstack.provision.model.rundeck.Execution;
import org.opendevstack.provision.model.rundeck.Job;
import org.opendevstack.provision.services.BitbucketAdapter;
import org.opendevstack.provision.services.ConfluenceAdapter;
import org.opendevstack.provision.services.JiraAdapter;
import org.opendevstack.provision.services.MailAdapter;
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.storage.LocalStorage;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * End to end testcase with real result data - only mock is the 
 * RestClient - to feed the json 
 * @author utschig
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBoot.class)
@DirtiesContext
public class E2EProjectAPIControllerTest
{
    private static Logger e2eLogger = LoggerFactory.getLogger(
            E2EProjectAPIControllerTest.class);
    
    @InjectMocks
    @Autowired
    private JiraAdapter realJiraAdapter;
    
    @InjectMocks
    @Autowired 
    private ConfluenceAdapter realConfluenceAdapter;
    
    @InjectMocks
    @Autowired 
    private BitbucketAdapter realBitbucketAdapter;

    @InjectMocks
    @Autowired
    private RundeckAdapter realRundeckAdapter;
    
    @Mock
    private RestClient mockRestClient;

    @InjectMocks
    @Autowired
    private ProjectApiController apiController;

    @Autowired
    private LocalStorage realStorageAdapter;

    @Autowired
    private MailAdapter realMailAdapter;
    
    private MockMvc mockMvc;
    
    // directory containing all the e2e test data
    private static File testDataDir = 
            new File("src/test/resources/e2e/");
    
    // results directory
    private static File resultsDir =
            new File(testDataDir, "results");
    
    // do NOT delete on cleanup
    private static List<String> excludeFromCleanup =
            Arrays.asList ( "20190101171023-LEGPROJ.txt" );
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(apiController)
                .build();
        
        // setup storage against test directory
        realStorageAdapter.setLocalStoragePath
            (resultsDir.getPath());
        
        // disable mail magic
        realMailAdapter.isMailEnabled = false;
    }
    
    @AfterClass 
    public static void cleanUp () throws Exception
    {
        for (File fresult : resultsDir.listFiles()) 
        {
            if (fresult.isDirectory() || 
                    excludeFromCleanup.contains(fresult.getName())) 
            {
                continue;
            }
            e2eLogger.debug("Deleting file {} result: {}",
                    fresult.getName(), fresult.delete());
        }
    }
   
    /**
     * Test positive - e2e new project - no quickstarters
     */
    @Test
    public void testProvisionNewSimpleProjectE2E() throws Exception
    {
        testProvisionNewSimpleProjectInternal(false);
    }

    /**
     * Test negative - e2e new project - no quickstarters,
     * rollback any external changes - bugtracker, scm,...
     */
    @Test
    public void testProvisionNewSimpleProjectE2EFail() throws Exception
    {
        cleanUp();
        testProvisionNewSimpleProjectInternal(true);
    }

    public void testProvisionNewSimpleProjectInternal(boolean fail) 
            throws Exception
    {
        // read the request
        OpenProjectData data =
                readTestData("ods-create-project-request", 
                        OpenProjectData.class);
        
        // jira server create project response
        FullJiraProject jiraProject = 
                readTestData("jira-create-project-response", 
                        FullJiraProject.class);
        
        Mockito.when(mockRestClient.callHttp(
                contains(realJiraAdapter.getAdapterApiUri() + "/project"), 
                any(FullJiraProject.class), 
                anyBoolean(), 
                eq(RestClient.HTTP_VERB.POST), 
                eq(FullJiraProject.class))).thenReturn(jiraProject);
        
        // session id
        Mockito.doNothing().when(mockRestClient).getSessionId(null);
        
        // get confluence blueprints
        List<Blueprint> blList = 
              readTestDataTypeRef(
                      "confluence-get-blueprints-response", 
                      new TypeReference<List<Blueprint>>(){});

        Mockito.when(mockRestClient.callHttpTypeRef(
                contains("dialog/web-items"), 
                eq(null), anyBoolean(), 
                eq(RestClient.HTTP_VERB.GET), 
                any())).thenReturn(blList);

        // get jira servers for confluence space
        List<JiraServer> jiraservers = 
                readTestDataTypeRef(
                        "confluence-get-jira-servers-response", 
                        new TypeReference<List<JiraServer>>(){});
        
        Mockito.when(mockRestClient.callHttpTypeRef(
                contains("jiraanywhere/1.0/servers"), 
                eq(null), anyBoolean(), 
                eq(RestClient.HTTP_VERB.GET), 
                any())).thenReturn(jiraservers);

        // create confluence space
        SpaceData confluenceSpace =
                readTestData("confluence-create-space-response", 
                        SpaceData.class);
        
        Mockito.when(mockRestClient.callHttp(
                contains("space-blueprint/create-space"), 
                any(Space.class), 
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST), 
                eq(SpaceData.class))).
            thenReturn(confluenceSpace);
        
        // bitbucket main project creation
        BitbucketProjectData bitbucketProjectData = 
                readTestData("bitbucket-create-project-response",
                        BitbucketProjectData.class);
        
        Mockito.when(mockRestClient.callHttp(
                contains(realBitbucketAdapter.getAdapterApiUri()),
                any (BitbucketProject.class), 
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST), 
                eq(BitbucketProjectData.class))).
            thenReturn(bitbucketProjectData);
        
        // bitbucket aux repo creation - oc-config
        RepositoryData bitbucketRepositoryDataOCConfig =
                readTestData("bitbucket-create-repo-occonfig-response", 
                        RepositoryData.class);

        Repository occonfigRepo = new Repository();
            occonfigRepo.setName(bitbucketRepositoryDataOCConfig.getName());
            
        Mockito.when(mockRestClient.callHttp(
                contains(realBitbucketAdapter.getAdapterApiUri()),
                refEq(occonfigRepo, "adminGroup", "userGroup"),
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST), 
                eq(RepositoryData.class))).
            thenReturn(bitbucketRepositoryDataOCConfig);
        
        // bitbucket aux repo creation - design repo
        RepositoryData bitbucketRepositoryDataDesign = 
                readTestData("bitbucket-create-repo-design-response", 
                        RepositoryData.class);
    
        Repository designRepo = new Repository();
            designRepo.setName(bitbucketRepositoryDataDesign.getName());
            
        Mockito.when(mockRestClient.callHttp(
                contains(realBitbucketAdapter.getAdapterApiUri()),
                refEq(designRepo, "adminGroup", "userGroup"),
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST), 
                eq(RepositoryData.class))).
            thenReturn(bitbucketRepositoryDataDesign);
        
        // basic auth rundeck
        Mockito.doNothing().when(mockRestClient).
            callHttpBasicFormAuthenticate(anyString());

        // populate the rundeck jobs
        List<Job> jobList = 
                readTestDataTypeRef("rundeck-get-jobs-response",
                        new TypeReference<List<Job>>(){});
        
        // will cause cleanup
        String rundeckUrl = 
                realRundeckAdapter.getAdapterApiUri() + "/project/";
        if (fail)
        {
            Mockito.when(mockRestClient.callHttpTypeRef(
                    contains(rundeckUrl),
                    anyMap(), anyBoolean(), 
                    eq(RestClient.HTTP_VERB.GET),
                    any())).
                thenThrow(new IOException("Rundeck TestFail"));
        } else 
        {
            Mockito.when(mockRestClient.callHttpTypeRef(
                    contains(rundeckUrl),
                    anyMap(), anyBoolean(), 
                    eq(RestClient.HTTP_VERB.GET),
                    any())).
                thenReturn(jobList);
        }
        
        // rundeck create-projects job execution
        ExecutionsData execution = 
                readTestData("rundeck-create-project-response", 
                        ExecutionsData.class);
        
        Mockito.when(mockRestClient.callHttp(
                contains("job/00f767ef-347f-480e-8ad3-bf2aed3abf5d/run"),
                any(Execution.class),
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST),
                eq(ExecutionsData.class))).
            thenReturn(execution);
                
        // create the ODS project
        MvcResult resultProjectCreationResponse = 
                mockMvc.perform(
                        post("/api/v2/project").content(
                                ProjectApiControllerTest.asJsonString(data))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print()).andReturn();

        if (!fail) 
        {
            assertEquals(
                    MockHttpServletResponse.SC_OK,
                    resultProjectCreationResponse.getResponse().getStatus());
        } else
        {
            assertEquals(
                    MockHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    resultProjectCreationResponse.getResponse().getStatus());

            Mockito.verify(mockRestClient, times(5)).callHttp(anyString(),
                eq(null), anyBoolean(), eq(HTTP_VERB.DELETE), eq(null));

            // delete jira project
            Mockito.verify(mockRestClient, times(1)).callHttp(
                    contains(realJiraAdapter.getAdapterApiUri()),
                    eq(null), anyBoolean(), eq(HTTP_VERB.DELETE), eq(null));

            // delete confluence space
            Mockito.verify(mockRestClient, times(1)).callHttp(
                    contains(realConfluenceAdapter.getAdapterApiUri()),
                    eq(null), anyBoolean(), eq(HTTP_VERB.DELETE), eq(null));

            // delete repos and bitbucket project
            Mockito.verify(mockRestClient, times(3)).callHttp(
                    contains(realBitbucketAdapter.getAdapterApiUri()),
                    eq(null), anyBoolean(), eq(HTTP_VERB.DELETE), eq(null));

            return;
        }
            
        // get the project thru its key
        MvcResult resultProjectGetResponse = 
                mockMvc.perform(
                        get("/api/v2/project/" + data.projectKey)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk())
                    .andDo(MockMvcResultHandlers.print()).andReturn();
        
        // verify responses
        assertEquals(
                resultProjectCreationResponse.getResponse().
                    getContentAsString(),
                resultProjectGetResponse.getResponse().
                    getContentAsString());

        OpenProjectData resultProject =
                new ObjectMapper().readValue(
                        resultProjectGetResponse.getResponse().
                            getContentAsString(),
                        OpenProjectData.class);
        
        // verify the execution
        assertEquals(1, resultProject.lastExecutionJobs.size());
        assertEquals(
                execution.getPermalink(),
                resultProject.lastExecutionJobs.iterator().next());
        
        // verify 2 repos are created
        assertEquals(2, resultProject.repositories.size());
    }


    /**
     * Test positive new quickstarter
     */
    @Test
    public void testQuickstarterProvisionOnNewOpenProject ()
            throws Exception 
    {
        testQuickstarterProvisionOnNewOpenProject(false);
    }

    /**
     * Test NEGATIVE new quickstarter - rollback ONE created repo
     */
    @Test
    public void testQuickstarterProvisionOnNewOpenProjectFail ()
            throws Exception 
    {
        testQuickstarterProvisionOnNewOpenProject(true);
    }

    public void testQuickstarterProvisionOnNewOpenProject (boolean fail) 
            throws Exception 
    {
        // read the request
        OpenProjectData dataUpdate =
                readTestData("ods-update-project-python-qs-request", 
                        OpenProjectData.class);

        // if project does not exist, create it thru the test
        if (realStorageAdapter.getProject(dataUpdate.projectKey) == null)
        {
            testProvisionNewSimpleProjectE2E();
        }
        
        OpenProjectData currentlyStoredProject =
                realStorageAdapter.getProject(dataUpdate.projectKey);
        
        assertNull(currentlyStoredProject.quickstarters);

        // bitbucket repo creation for new quickstarter
        RepositoryData bitbucketRepositoryDataQSRepo =
                readTestData("bitbucket-create-repo-python-qs-response", 
                        RepositoryData.class);

        Repository qsrepo = new Repository();
            qsrepo.setName(bitbucketRepositoryDataQSRepo.getName());
            
        Mockito.when(mockRestClient.callHttp(
                contains(realBitbucketAdapter.getAdapterApiUri()),
                refEq(qsrepo, "adminGroup", "userGroup"),
                anyBoolean(),
                eq(RestClient.HTTP_VERB.POST), 
                eq(RepositoryData.class))).
            thenReturn(bitbucketRepositoryDataQSRepo);

        // get the rundeck jobs
        List<Job> jobList = 
                readTestDataTypeRef("rundeck-get-jobs-response",
                        new TypeReference<List<Job>>(){});
        
        Mockito.when(mockRestClient.callHttpTypeRef(
                contains(realRundeckAdapter.getAdapterApiUri() + "/project/"),
                anyMap(), anyBoolean(), 
                eq(RestClient.HTTP_VERB.GET),
                any())).
            thenReturn(jobList);

        // rundeck python job execution
        ExecutionsData execution = 
                readTestData("rundeck-create-python-qs-response", 
                        ExecutionsData.class);
        
        if (!fail) 
        {
            Mockito.when(mockRestClient.callHttp(
                    contains("job/9992a587-959c-4ceb-8e3f-c1390e40c582/run"),
                    any(Execution.class),
                    anyBoolean(),
                    eq(RestClient.HTTP_VERB.POST),
                    eq(ExecutionsData.class))).
                thenReturn(execution);
        } else {
            Mockito.when(mockRestClient.callHttp(
                    contains("job/9992a587-959c-4ceb-8e3f-c1390e40c582/run"),
                    any(Execution.class),
                    anyBoolean(),
                    eq(RestClient.HTTP_VERB.POST),
                    eq(ExecutionsData.class))).
                thenThrow(new IOException("Rundeck provision job failed"));
        }
        
        // update the project with the new quickstarter
        MvcResult resultUpdateResponse = 
                mockMvc.perform(
                        put("/api/v2/project").content(
                     ProjectApiControllerTest.asJsonString(dataUpdate))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultHandlers.print()).andReturn();

        if (fail) {
            assertEquals(
                    MockHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    resultUpdateResponse.getResponse().getStatus());

            // delete repos and bitbucket project
            Mockito.verify(mockRestClient, times(1)).callHttp(
                    contains(realBitbucketAdapter.getAdapterApiUri()),
                    eq(null), anyBoolean(), eq(HTTP_VERB.DELETE), eq(null));

            return;
        } else 
        {
            assertEquals(
                    MockHttpServletResponse.SC_OK,
                    resultUpdateResponse.getResponse().getStatus());
        }
        
        // get the inlined body result
        String resultUpdateData =
                resultUpdateResponse.getResponse().
                getContentAsString();
        assertNotNull(resultUpdateData);
        
        // convert into a project pojo
        OpenProjectData resultProject =
                new ObjectMapper().readValue(resultUpdateData,
                        OpenProjectData.class);
        
        List<Map<String,String>> createdQuickstarters =
                resultProject.quickstarters;
                
        assertNotNull(createdQuickstarters);
        assertEquals(1, createdQuickstarters.size());

        assertEquals(1,  resultProject.lastExecutionJobs.size());
        assertEquals(execution.getPermalink(),
                resultProject.lastExecutionJobs.iterator().next());
    }

    /**
     * Test legacy upgrade e2e
     */
    @Test
    public void testLegacyProjectUpgradeOnGet () 
            throws Exception 
    {
        // get the project thru its key
        MvcResult resultLegacyProjectGetResponse = 
                mockMvc.perform(
                        get("/api/v2/project/LEGPROJ")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk())
                    .andDo(MockMvcResultHandlers.print()).andReturn();
        
        OpenProjectData resultLegacyProject =
                new ObjectMapper().readValue(
                        resultLegacyProjectGetResponse.getResponse().
                            getContentAsString(),
                        OpenProjectData.class);
        
        // verify 4 repos are there - 2 base 2 qs
        assertEquals(4, resultLegacyProject.
                repositories.size());
        
        // verify 2 quickstarters are there
        assertEquals(2, resultLegacyProject.
                quickstarters.size());        
    }

    /*
     * internal test helpers
     */
    
    private <T> T readTestData(String name, Class<T> returnType) 
            throws Exception
    {
        return new ObjectMapper().readValue(
                findTestFile(name), returnType);
    }

    private <T> T readTestDataTypeRef(String name, TypeReference<T> returnType) 
            throws Exception
    {
        return new ObjectMapper().readValue(
                findTestFile(name), returnType);
    }

    private File findTestFile (String fileName) throws IOException 
    {
        Preconditions.checkNotNull(fileName, "File cannot be null");
        if (!fileName.endsWith(".json")) 
        {
            fileName = fileName + ".json";
        }
        File dataFile = new File (testDataDir, fileName);
        if (!dataFile.exists()) {
            throw new IOException("Cannot find testfile with name:" +
                    dataFile.getName());
        }
        return dataFile;
    }
}
