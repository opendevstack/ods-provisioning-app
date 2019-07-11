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
package org.opendevstack.provision.util.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Simple cookie jar implementation to store cookies for all for session bound APIs
 *
 * @author Torsten Jaeschke
 */

public class SimpleCookieJar implements CookieJar {

  /**
   * Set to store the cookies
   */
  private Map<HttpUrl, List<Cookie>> cookies = new HashMap<>();

  /**
   * Save cookies from response
   */
  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    this.cookies.put(url, cookies);
  }

  /**
   * Load cookies to include in request
   */
  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    return cookies.getOrDefault(url, new ArrayList<>());
  }
}
