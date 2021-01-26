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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendevstack.provision.authentication.crowd.CrowdAuthenticationManager;
import org.opendevstack.provision.model.OpenProjectData;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
public class MailAdapterTest {

  private MailAdapter mailAdapter;

  @Mock private CrowdAuthenticationManager crowdAuthenticationManager;

  @Mock private JavaMailSender mailSender;

  @BeforeEach
  public void setUp() {
    mailAdapter = new MailAdapter(mailSender);
    mailAdapter.manager = crowdAuthenticationManager;
  }

  @Test
  public void notifyUsersAboutProjectMailEnabled() {
    mailAdapter.setMailEnabled(true);
    mailAdapter.notifyUsersAboutProject(new OpenProjectData());
  }

  @Test
  public void notifyUsersAboutProjectMailDisabled() {
    mailAdapter.setMailEnabled(false);
    mailAdapter.notifyUsersAboutProject(new OpenProjectData());
    Mockito.verify(mailSender, Mockito.never()).send(mimeMessage -> {});
  }
}
