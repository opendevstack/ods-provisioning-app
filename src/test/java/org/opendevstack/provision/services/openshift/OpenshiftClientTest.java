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
package org.opendevstack.provision.services.openshift;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class OpenshiftClientTest {

    @Test
    public void testOpenshiftClientReturnsProjectKeys() throws IOException {

        byte[] jsonContent =

                Objects.requireNonNull(getClass().getResourceAsStream("/openshift/openshift-projects.json"))
                        .readAllBytes();
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(
                get("/apis/project.openshift.io/v1/projects")
                        .willReturn(aResponse().withBody(jsonContent)));
        OpenshiftClient ocClient = new OpenshiftClient(wireMockServer.baseUrl());

        Set<String> projects = ocClient.projects();

        assertEquals("testproject-dev", projects.iterator().next());

        wireMockServer.verify(
                exactly(1), getRequestedFor(urlEqualTo("/apis/project.openshift.io/v1/projects")));
        wireMockServer.stop();
    }
}
