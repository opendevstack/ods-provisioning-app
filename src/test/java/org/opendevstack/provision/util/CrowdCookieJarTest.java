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

package org.opendevstack.provision.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendevstack.provision.SpringBoot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * @author Torsten Jaeschke
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, classes = SpringBoot.class)
@DirtiesContext
public class CrowdCookieJarTest {

  private CrowdCookieJar crowdCookieJar;

  @Before
  public void setUp() {
    crowdCookieJar = new CrowdCookieJar();
  }

  @Test
  public void saveFromResponse() throws Exception {
    crowdCookieJar.saveFromResponse(getUrl(), getCookies());
    assertTrue(crowdCookieJar.loadForRequest(getUrl()).equals(getCookies()));
  }

  @Test
  public void loadForRequest() throws Exception {
    crowdCookieJar.saveFromResponse(getUrl(), getCookies());
    assertTrue(crowdCookieJar.loadForRequest(getUrl()).equals(getCookies()));
  }

  @Test
  public void loadForRequestWClear() throws Exception {
    crowdCookieJar.clear();
    assertEquals(0, crowdCookieJar.loadForRequest(null).size());
  }

  
  @Test
  public void loadForRequestWithoutCookies() throws Exception {
    assertTrue(crowdCookieJar.loadForRequest(getUrl()).size() == 0);
  }

  @Test
  public void addCrowdCookie() throws Exception {
    crowdCookieJar.setDomain("localhost");
    crowdCookieJar.saveFromResponse(getUrl(), getCookies());
    crowdCookieJar.addCrowdCookie("test");
    List<Cookie> cookies = crowdCookieJar.loadForRequest(getUrl());
    
    for (Cookie cookie : cookies) {
      if (cookie.name().equalsIgnoreCase("crowd.token_key")) {
        assertTrue(true);
      }
    }
  }

  private HttpUrl getUrl() {
    HttpUrl url = new HttpUrl.Builder().host("localhost").scheme("http").build();
    return url;
  }

  private List<Cookie> getCookies() {
    List<Cookie> cookies = new ArrayList<>();
    cookies.add(new Cookie.Builder().name("ck1").value("ck1").domain("localhost").build());
    cookies.add(new Cookie.Builder().name("ck2").value("ck2").domain("localhost").build());
    return cookies;
  }
}
