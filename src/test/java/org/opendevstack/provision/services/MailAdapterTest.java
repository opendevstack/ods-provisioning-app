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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.model.OpenProjectData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("crowd")
public class MailAdapterTest {

  @Autowired private MailAdapter mailAdapter;

  @Mock private CrowdAuthenticationManager crowdAuthenticationManager;

  @Mock private JavaMailSender mailSender;

  @BeforeEach
  public void setUp() {
    mailAdapter.manager = crowdAuthenticationManager;
  }

  @Test
  public void notifyUsersAboutProject() {
    MailAdapter spyAdapter = new MailAdapter(mailSender);
    Mockito.doNothing().when(mailSender).send(any(MimeMessage.class));

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
    assertFalse(message.trim().isEmpty());
  }
}
