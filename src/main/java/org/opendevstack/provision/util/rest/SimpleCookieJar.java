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

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple cookie jar implementation to store cookies for all for session bound APIs
 *
 * @author Torsten Jaeschke
 */

public class SimpleCookieJar implements CookieJar {

  private static final Logger logger = LoggerFactory.getLogger(SimpleCookieJar.class);

  /**
   * Set to store the cookies
   */
  private Map<String, List<Cookie>> cookies = new HashMap<>();

  /**
   * Save cookies from response
   */
  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    logger.debug("Save cookies for host[{}]", url.host());
    this.cookies.put(url.host(), cookies);
  }

  /**
   * Load cookies to include in request
   */
  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    logger.debug("Load cookies for host[{}]", url.host());
    return cookies.getOrDefault(url.host(), new ArrayList<>());
  }
}
