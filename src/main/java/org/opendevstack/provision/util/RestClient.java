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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
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

  private static final MediaType JSON_MEDIA_TYPE = MediaType
	      .parse("application/json; charset=utf-8");

  private CrowdCookieJar cookieJar;

  private int connectTimeout = 30;

  private int readTimeout = 60;
  
  private Map<String, OkHttpClient> cache = new HashMap<>();

  private static final List<Integer> RETRY_HTTP_CODES = 
	new ArrayList<>(Arrays.asList(401, 403, 500));

  @Autowired
  private CustomAuthenticationManager manager;

  public enum HTTP_VERB {
	  PUT, 
	  POST,
	  GET,
	  HEAD
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
    builder.cookieJar(cookieJar).connectTimeout(connectTimeout, TimeUnit.SECONDS)
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
			executeHttpCall(url, null, null, false, HTTP_VERB.HEAD);
		} catch (HttpException httpX) {
			if (RETRY_HTTP_CODES.contains(httpX.getResponseCode())) {
				executeHttpCall(url, null, null, false, HTTP_VERB.HEAD);
			} else {
				throw httpX;
			}
		}
	}

	public <T> T callHttp(String url, Object input, String crowdCookieValue,
						  boolean directAuth, HTTP_VERB verb, Class<T> returnType)
			throws IOException {
		String respBody = callHttpString(url, input, crowdCookieValue, directAuth, verb);
		if (respBody == null || returnType == null) {
			return null;
		}
		return new ObjectMapper().readValue(respBody, returnType);
	}

	public <T> T callHttpTypeRef(String url, Object input, String crowdCookieValue,
								 boolean directAuth, HTTP_VERB verb, TypeReference<T> returnTypeRef)
			throws IOException {
		String respBody = callHttpString(url, input, crowdCookieValue, directAuth, verb);
		if (respBody == null || returnTypeRef == null) {
			return null;
		}
		return new ObjectMapper().readValue(respBody, returnTypeRef);
	}

	public String callHttpString(String url, Object input, String crowdCookieValue, boolean directAuth, HTTP_VERB verb) throws IOException {
		String respBody;
		try {
			respBody = executeHttpCall(url, input, crowdCookieValue, directAuth, verb);

		} catch (HttpException httpException) {
			if (RETRY_HTTP_CODES.contains(httpException.getResponseCode())) {
				logger.debug("401 - retrying with direct auth");
				respBody = executeHttpCall(url, input, crowdCookieValue, true, verb);
			} else {
				throw httpException;
			}
		}
		return respBody;
	}

	private String executeHttpCall(String url, Object input, String crowdCookieValue, boolean directAuth, HTTP_VERB verb) throws IOException {
		Preconditions.checkNotNull(url, "Url cannot be null");
		Preconditions.checkNotNull(verb, "HTTP Verb cannot be null");

		String json;

		logger.debug("Calling url: {}", url);

		if (input == null) {
			json = "";
			logger.debug("Null payload");
		} else if (input instanceof String) {
			json = (String) input;
			logger.debug("Passed String rest object: [{}]", json);
		} else {
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			json = ow.writeValueAsString(input);
			logger.debug("Converted rest object: {}", json);
		}

		RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);

		Request.Builder builder = createRequestBuilder(url, verb, body);

		Response response;
		if (directAuth) {
			String currentUser =
					SecurityContextHolder.getContext().getAuthentication().getName();
			logger.debug("Authenticating rest call with {}", currentUser);
			String credentials =
					Credentials.basic(
							currentUser, manager.getUserPassword());
			builder = builder.addHeader("Authorization", credentials);
			response = getClientFresh(crowdCookieValue).newCall(builder.build()).execute();
		} else {
			response = getClient(crowdCookieValue).newCall(builder.build()).execute();
		}

		String respBody;
		if (response.body() != null) {
			respBody = response.body().string();
		} else {
			respBody = null;
		}
		response.close();
		if (logger.isDebugEnabled()) {
			if (StringUtils.isBlank(respBody)) {
				logger.debug("{} {} > {}", verb, url, response.code());
			} else {
				logger.debug("{} {} > {}: \n{}", verb, url, response.code(), respBody);
			}
		}

		if (response.code() < 200 || response.code() >= 300) {
			throw new HttpException(response.code(),
					"Could not " + verb + " > " + url + " : " + respBody);
		}
		return respBody;
	}

	private Request.Builder createRequestBuilder(String url, HTTP_VERB verb, RequestBody body) {
		Request.Builder builder = new Request.Builder();
		builder.url(url).addHeader("X-Atlassian-Token", "no-check").addHeader("Accept", "application/json");

		if (HTTP_VERB.PUT.equals(verb)) {
			builder = builder.put(body);
		} else if (HTTP_VERB.GET.equals(verb)) {
			builder = builder.get();
		} else if (HTTP_VERB.POST.equals(verb)) {
			builder = builder.post(body);
		} else if (HTTP_VERB.HEAD.equals(verb)) {
			builder = builder.head();
		}
		return builder;
	}

	public void callHttpBasicFormAuthenticate(String url) throws IOException
  {
	Preconditions.checkNotNull(url, "Url cannot be null");
	Preconditions.checkNotNull(SecurityContextHolder.getContext().getAuthentication(),
		"Cannot auth with null principal");
    CrowdUserDetails userDetails =
        (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    String username = userDetails.getUsername();
    String password = manager.getUserPassword();
    
    RequestBody body =
        new FormBody.Builder().add("j_username", username).add("j_password", password).build();
    Request request = new Request.Builder()
        .url(url).post(body)
        .build();
    try (Response response = getClient(null).newCall(request).execute();)
    {
		if (!response.isSuccessful() || (response.body() != null && response.body().string().contains("Invalid username and password"))) {
			throw new IOException("Could not authenticate: " + username +
					" : " + response.body());
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
  
  public void removeClient (String crowdCookieValue) {
	  if (crowdCookieValue == null) {
		  return;
	  }
	  cache.remove(crowdCookieValue);
  }
}
