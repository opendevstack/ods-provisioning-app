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

package org.opendevstack.provision.authentication.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.filter.SSOAuthProcessingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.crowd.integration.http.HttpAuthenticator;
import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationToken;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class SSOAuthProcessingFilterTest
{

    @Mock
    HttpAuthenticator authenticator;

    @Autowired
    @InjectMocks
    SSOAuthProcessingFilter filter;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void storeCrowdToken() throws Exception
    {
        CrowdSSOAuthenticationToken token = new CrowdSSOAuthenticationToken(
                "token");

        HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito
                .mock(HttpServletResponse.class);

        assertTrue(
                filter.storeTokenIfCrowd(request, response, token));
        assertFalse(
                filter.storeTokenIfCrowd(request, response, null));
    }

    @Test
    public void testSuccessfullAuth() throws Exception
    {
        HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito
                .mock(HttpServletResponse.class);

        CrowdSSOAuthenticationToken token = new CrowdSSOAuthenticationToken(
                "token");

        filter.successfulAuthentication(request, response, null,
                token);
    }
}
