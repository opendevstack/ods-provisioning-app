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

package org.opendevstack.provision.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.model.OpenProjectData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
@ActiveProfiles("crowd")
public class MailAdapterTest {
  @Mock JavaMailSender mailSender;

  @InjectMocks @Autowired MailAdapter mailAdapter;

  @Autowired CrowdAuthenticationManager manager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mailAdapter.manager = manager;
  }

  @Test
  public void notifyUsersAboutProject() {
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    Mockito.doNothing().when(mailSender).send(any(MimeMessage.class));
    spyAdapter = new MailAdapter(mailSender);

    spyAdapter.notifyUsersAboutProject(new OpenProjectData());
  }

  @Test
  public void notifyUsersMailDisabled() {
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    spyAdapter.isMailEnabled = false;
    Mockito.verify(mailSender, Mockito.never()).send(any(MimeMessage.class));
  }

  @Test
  public void notifyUsersAboutProjectWhenCrowdUserDetailsIsNull() {
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);
    Mockito.doNothing().when(mailSender).send(any(MimeMessage.class));

    spyAdapter.notifyUsersAboutProject(new OpenProjectData());
  }

  @Test
  public void testMailBuild() {
    Mockito.doNothing().when(mailSender).send(any(MimeMessage.class));
    MailAdapter spyAdapter = Mockito.spy(mailAdapter);

    String message = spyAdapter.build(new OpenProjectData());
    assertNotNull(message);
    assertTrue(message.trim().length() > 0);
  }
}
