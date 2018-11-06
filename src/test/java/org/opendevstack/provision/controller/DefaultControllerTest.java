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

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.opendevstack.provision.SpringBoot;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.authentication.TestAuthentication;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.AboutChangesData.AboutRecordData;
import org.opendevstack.provision.services.RundeckAdapter;
import org.opendevstack.provision.services.StorageAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import net.sf.ehcache.CacheManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

/**
 * Created by TJA on 29.06.2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = SpringBoot.class)
@DirtiesContext
@WithMockUser(username = "test")
public class DefaultControllerTest {

  private MockMvc mockMvc;

  @Autowired
  DefaultController defaultController;

  @Mock
  RundeckAdapter rundeckAdapter;

  @Mock
  CustomAuthenticationManager customAuthenticationManager;

  @Mock
  StorageAdapter storageAdapter;

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        //.apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
  }

  @Test
  public void rootRedirect() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
  }

  @Test
  @WithMockUser(username = "test")
  public void homeWithoutAuth() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn(null);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/home"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  @WithMockUser(username = "test")
  public void homeWithAuth() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn("logged_in");
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/home"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  @WithMockUser(username = "test")
  public void provisionWithAuth() throws Exception 
  {
	Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn("logged_in");
    Mockito.when(rundeckAdapter.getQuickstarter()).thenReturn(new ArrayList<>());
    defaultController.setRundeckAdapter(rundeckAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
	SecurityContextHolder.getContext().setAuthentication(new TestAuthentication());
    mockMvc.perform(get("/provision"))
    	.andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk());
  }
  
  @Test
  public void provisionWithoutAuth() throws Exception {
    Mockito.when(rundeckAdapter.getQuickstarter()).thenReturn(new ArrayList<>());
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn(null);
    defaultController.setRundeckAdapter(rundeckAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/provision"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andDo(MockMvcResultHandlers.print());
  }

  @Test
  public void login() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn(null);
    Mockito.when(storageAdapter.listProjectHistory()).thenReturn(new HashMap<>());
    defaultController.setStorageAdapter(storageAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/login"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
  }

  @Test
  public void history() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn(null);
    Mockito.when(storageAdapter.listProjectHistory()).thenReturn(new HashMap<>());
    defaultController.setStorageAdapter(storageAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/history"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
  }

  @Test
  public void historyWithAuth() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn("logged_in");
    Mockito.when(storageAdapter.listProjectHistory()).thenReturn(new HashMap<>());
    defaultController.setStorageAdapter(storageAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/history"))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }
  
  @Test
  public void logoutPage() throws Exception {
    mockMvc.perform(get("/logout"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
  }

  @Test
  public void aboutWithAuth() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn("logged_in");
    Mockito.when(storageAdapter.listAboutChangesData()).thenReturn(new AboutChangesData());
    defaultController.setStorageAdapter(storageAdapter);
    defaultController.setCustomAuthenticationManager(customAuthenticationManager);
    mockMvc.perform(get("/about"))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }
  
  @Test
  public void aboutWithoutAuth() throws Exception {
    Mockito.when(customAuthenticationManager.getUserPassword()).thenReturn(null);
    mockMvc.perform(get("/about"))
    	.andExpect(MockMvcResultMatchers.status().is3xxRedirection());
  }
}