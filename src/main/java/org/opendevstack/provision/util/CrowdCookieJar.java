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

import com.google.common.collect.Lists;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom cookie jar with the ability to inject a crowd cookie manually if the domains are
 * different.
 *
 * @author Torsten Jaeschke
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CrowdCookieJar implements CookieJar {

  String domain;
  private Set<Cookie> cookies = new HashSet<>();

  private String crowdSSOCookieName;

  /**
   * Save cookies from response
   *
   * @param url
   * @param cookies
   */
  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    this.cookies.clear();
    this.cookies.addAll(cookies);
  }

  /**
   * Load cookies to include in request
   *
   * @param url
   * @return
   */
  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    if (cookies != null) {
      return Lists.newArrayList(cookies);
    }
    return new ArrayList<>();
  }

  /**
   * Add a custom crwod cookie
   *
   * @param cookieValue
   */
  public void addCrowdCookie(String cookieValue) {
    Cookie.Builder cookieBuilder = new Cookie.Builder();
    Cookie crowdCookie =
        cookieBuilder
            .name(crowdSSOCookieName)
            .domain(domain)
            .path("/")
            .httpOnly()
            .value(cookieValue)
            .build();
    cookies.add(crowdCookie);
  }

  @Value("${atlassian.domain}")
  public void setDomain(String domain) {
    this.domain = domain;
  }

  @Value("${crowd.sso.cookie.name}")
  public void setSSOCookieName(String cookieName) {
    crowdSSOCookieName = cookieName;
  }

  public void clear() {
    cookies = new HashSet<>();
  }
}
