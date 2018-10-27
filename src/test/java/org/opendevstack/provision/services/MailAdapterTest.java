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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.model.ProjectData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class MailAdapterTest {

  @Mock
  JavaMailSender mailSender;

  @Mock
  CrowdUserDetails details;

  @InjectMocks
  @Autowired
  MailAdapter mailAdapter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void notifyUsersAboutProject() throws Exception {
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    Mockito.doNothing().when(mailSender).send(Matchers.any(MimeMessage.class));
    Mockito.when(details.getEmail()).thenReturn("test@example.com");
    spyAdapter = new MailAdapter(mailSender);
    spyAdapter.setCrowdUserDetails(details);
    
    spyAdapter.notifyUsersAboutProject(new ProjectData());
  }

  @Test
  public void notifyUsersAboutProjectWhenCrowdUserDetailsIsNull() throws Exception {
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    spyAdapter.setCrowdUserDetails(null);
    Mockito.doReturn(details).when(spyAdapter).getCrowdUserDetailsFromContext();
    Mockito.when(details.getEmail()).thenReturn("test@example.com");
    Mockito.doNothing().when(mailSender).send(Matchers.any(MimeMessage.class));

    spyAdapter.notifyUsersAboutProject(new ProjectData());

    Mockito.verify(spyAdapter).getCrowdUserDetailsFromContext();
  }
  
  @Test
  public void testMailBuild () throws Exception {
    Mockito.doNothing().when(mailSender).send(Matchers.any(MimeMessage.class));
    Mockito.when(details.getEmail()).thenReturn("test@example.com");
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    spyAdapter.setCrowdUserDetails(details);

    String message = spyAdapter.build(new ProjectData());
    assertNotNull(message);
    assertTrue(message.trim().length() > 0);
  }
}
