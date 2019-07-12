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
package org.opendevstack.provision.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction to handle the OkHTTP client and use it in the same way from different classes to
 * prevent redundant code.
 *
 * @author Torsten Jaeschke
 * @author Clemens Utschig
 */
@Component
public class RestClient {
  private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  CrowdCookieJar cookieJar;

  int connectTimeout = 30;

  int readTimeout = 60;

  private Map<String, OkHttpClient> cache = new HashMap<>();

  private static final List<Integer> RETRY_HTTP_CODES =
      new ArrayList<>(Arrays.asList(401, 403, 404, 409, 500));

  @Autowired IODSAuthnzAdapter manager;

  public enum HTTP_VERB {
    PUT,
    POST,
    GET,
    HEAD, DELETE
  }

  OkHttpClient getClient(String crowdCookie) {

    OkHttpClient client = cache.get(crowdCookie);
    if (client != null) {
      return client;
    }

    OkHttpClient.Builder builder = new Builder();
    if (null != crowdCookie) {
      cookieJar.addCrowdCookie(crowdCookie);
    }
    builder
        .cookieJar(cookieJar)
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS);
    client = builder.build();
    cache.put(crowdCookie, client);
    return client;
  }

  private OkHttpClient getClientFresh(String crowdCookie) {
    cache.remove(crowdCookie);
    cookieJar.clear();
    return getClient(null);
  }

  public void getSessionId(String url) throws IOException {
    try {
      callHttpInternal(url, null, false, HTTP_VERB.HEAD, null, null);
    } catch (HttpException httpX) {
      if (RETRY_HTTP_CODES.contains(httpX.getResponseCode())) {
        callHttpInternal(url, null, true, HTTP_VERB.HEAD, null, null);
      } else {
        throw httpX;
      }
    }
  }

  public <T> T callHttp(
      String url, Object input, boolean directAuth, HTTP_VERB verb, Class<T> returnType)
      throws HttpException, IOException {
    try {
      return callHttpInternal(url, input, directAuth, verb, returnType, null);
    } catch (HttpException httpException) {
      if (RETRY_HTTP_CODES.contains(httpException.getResponseCode())) {
        logger.debug("401 - retrying with direct auth");
        return callHttpInternal(url, input, true, verb, returnType, null);
      } else {
        throw httpException;
      }
    }
  }

  public <T> T callHttpWithDirectAuth(
          String url, Object input, HTTP_VERB verb, Class<T> returnType, String userName,
          String userPassword)
          throws IOException {
    try {
      CredentialsInfo credentials = new CredentialsInfo(userName,userPassword);
      return callHttpInternal(url, input, true, verb, returnType, null, credentials);
    } catch (HttpException httpException) {
      logger.error("callHttpWithDirectAuth failded",httpException);
      throw httpException;
    }
  }

  public <T> T callHttpTypeRef(
      String url, Object input, boolean directAuth, HTTP_VERB verb, TypeReference<T> returnType)
      throws IOException {
    try {
      return callHttpInternal(url, input, directAuth, verb, null, returnType);
    } catch (HttpException httpException) {
      if (RETRY_HTTP_CODES.contains(httpException.getResponseCode())) {
        logger.debug("401 - retrying with direct auth");
        return callHttpInternal(url, input, true, verb, null, returnType);
      } else {
        throw httpException;
      }
    }
  }



  public <T> T callHttpTypeRefWithDirectAuth(
      String url, Object input, HTTP_VERB verb, Class returnType,TypeReference<T> returnTypeRef, String userName,
      String userPassword)
      throws IOException {
    CredentialsInfo credentials = new CredentialsInfo(userName,userPassword);
    return callHttpInternal(url, input, true, verb, returnType, returnTypeRef, credentials);
  }

  private <T> T callHttpInternal(
      String url,
      Object input,
      boolean directAuth,
      HTTP_VERB verb,
      Class returnType,
      TypeReference returnTypeRef)
      throws HttpException, IOException {
    CredentialsInfo credentials = new CredentialsInfo(manager.getUserName(),manager.getUserPassword());
    return callHttpInternal(url, input, directAuth, verb, returnType, returnTypeRef, credentials);
  }

  private <T> T callHttpInternal(
      String url,
      Object input,
      boolean directAuth,
      HTTP_VERB verb,
      Class returnType,
      TypeReference returnTypeRef,
      CredentialsInfo credentials)
      throws IOException {
    Preconditions.checkNotNull(url, "Url cannot be null");
    Preconditions.checkNotNull(verb, "HTTP Verb cannot be null");

    String json = null;

    logger.debug("Calling url: " + url);

    if (input == null) {
      json = "";
      logger.debug("Null payload");
    } else if (input instanceof String) {
      json = (String) input;
      logger.debug("Passed String rest object: [{}]", json);
    } else if (input instanceof Map) {
      HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
      Map<String, String> paramMap = ((Map) input);
      for (Map.Entry<String, String> param : paramMap.entrySet()) {
        urlBuilder.addQueryParameter(param.getKey(), param.getValue());
      }
      return callHttpInternal(
          urlBuilder.toString(), null, directAuth, verb, returnType, returnTypeRef);
    } else {
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      json = ow.writeValueAsString(input);
      logger.debug("Converted rest object: {}", json);
    }

    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);

    Request.Builder builder = new Request.Builder();
    builder
        .url(url)
        .addHeader("X-Atlassian-Token", "no-check")
        .addHeader("Accept", "application/json");

    switch (verb) {
      case PUT:
        builder = builder.put(body);
        break;
      case GET:
        builder = builder.get();
        break;
      case POST:
        builder = builder.post(body);
        break;
      case HEAD:
        builder = builder.head();
                break;
            case DELETE:
                builder = builder.delete();
        break;
      default:
        builder = builder.head();
        break;
    }

    Response response = null;
    if (directAuth) {
      logger.debug("Authenticating rest call with user {}", credentials.getUserName());

      builder = builder.addHeader("Authorization", credentials.getCredentials());
      response = getClientFresh(manager.getToken()).newCall(builder.build()).execute();
    } else {
      response =
          getClient(manager != null ? manager.getToken() : null).newCall(builder.build()).execute();
    }

    String respBody = response.body().string();
    logger.debug(url + " > " + verb + " >> " + response.code() + ": \n" + respBody);

    if (response.code() < 200 || response.code() >= 300) {
      throw new HttpException(
          response.code(), "Could not " + verb + " > " + url + " : " + respBody);
    }

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    if (returnType == null && returnTypeRef == null) {
      return null;
    } else if (returnType != null) {
      if (returnType.isAssignableFrom(String.class)) {
        return (T) respBody;
      }
      return (T) objectMapper.readValue(respBody, returnType);
    } else {
      return (T) objectMapper.readValue(respBody, returnTypeRef);
    }
  }

  public void callHttpBasicFormAuthenticate(String url) throws IOException {
    Preconditions.checkNotNull(url, "Url cannot be null");
    String username = manager.getUserName();
    String password = manager.getUserPassword();
    callHttpBasicFormAuthenticate(url, username, password);
  }

  public void callHttpBasicFormAuthenticate(String url, String username, String password) throws IOException {
    RequestBody body =
        new FormBody.Builder()
            .add("j_username", username)
            .add("j_password",password )
            .build();
    Request request = new Request.Builder().url(url).post(body).build();
    try (Response response = getClient(null).newCall(request).execute(); ) {
      if (!response.isSuccessful()
          || response.body().string().contains("Invalid username and password")) {
        throw new IOException("Could not authenticate: " + username + " : " + response.body());
      }
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

  public void removeClient(String crowdCookieValue) {
    if (crowdCookieValue == null) {
      return;
    }
    cache.remove(crowdCookieValue);
  }
}
