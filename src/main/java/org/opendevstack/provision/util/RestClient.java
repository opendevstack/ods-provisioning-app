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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Abstraction to handle the OkHTTP client and use it in the same way from different classes to
 * prevent redundant code.
 *
 * @author Torsten Jaeschke
 */

@Component
public class RestClient {

  private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

  CrowdCookieJar cookieJar;

  int connectTimeout = 30;

  int readTimeout = 60;

  public OkHttpClient getClient() {
    return getClient(null);
  }

  public OkHttpClient getClient(String crowdCookie) {

    OkHttpClient.Builder builder = new Builder();
    if (null != crowdCookie) {
      cookieJar.addCrowdCookie(crowdCookie);
    }
    builder.cookieJar(cookieJar).connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS);
    return builder.build();
  }

  public void getSessionId(String url) throws IOException {
    try {
      Request req = new Request.Builder().url(url).get().build();
      Response resp = getClient().newCall(req).execute();
      resp.close();
    } catch (IOException ex) {
      logger.error("Error in getting session", ex);
      throw ex;
    }
  }

  @Autowired
  public void setCookieJar(CrowdCookieJar cookieJar) {
    this.cookieJar = cookieJar;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }
}
