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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
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

  CrowdCookieJar cookieJar;

  int connectTimeout = 30;

  int readTimeout = 60;
  
  private Map<String, OkHttpClient> cache = new HashMap<>();

  @Autowired
  CrowdUserDetailsService crowdUserDetailsService;

  @Autowired
  CustomAuthenticationManager manager;

  public static enum HTTP_VERB {
	  PUT, 
	  POST,
	  GET,
	  HEAD
  }  
 
  public static enum HTTP_POST_TYPE {
	  URL,
	  FORM
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

  public OkHttpClient getClientFresh(String crowdCookie) {
	cache.remove(crowdCookie);
    cookieJar.clear();
    return getClient(null);
  }  
  
  public void getSessionId(String url) throws IOException 
  {
	  try 
	  {
		  callHttpInternal(url, null, null, false, HTTP_VERB.HEAD, 
				null, null, null);
	  } catch (HttpException httpX) {
		  if (httpX.getResponseCode() != 401) 
		  {
			  throw httpX;
		  }
	  }
  }

  public <T> T callHttpPut(String url, Map<String,String> input, String crowdCookieValue, 
		boolean directAuth)
			throws JsonMappingException, HttpException, IOException
  {
	  return callHttpInternal(url, input, crowdCookieValue, directAuth, 
			HTTP_VERB.PUT, null, null, HTTP_POST_TYPE.URL);
  }
  
  
  public <T> T callHttp(String url, Object input, String crowdCookieValue, 
		  boolean directAuth, HTTP_VERB verb, Class<T> returnType)
			      throws JsonMappingException, HttpException, IOException
  {
	  return callHttpInternal(url, input, crowdCookieValue, directAuth, verb, returnType, null, null);
  }

  public <T> T callHttpTypeRef(String url, Object input, String crowdCookieValue, 
		  boolean directAuth, HTTP_VERB verb, TypeReference<T> returnType)
			      throws JsonMappingException, HttpException, IOException
  {
	  return callHttpInternal(url, input, crowdCookieValue, directAuth, verb, null, returnType, null);
  }
  
  private <T> T callHttpInternal(String url, Object input, String crowdCookieValue, 
		  boolean directAuth, HTTP_VERB verb, Class returnType, TypeReference returnTypeRef, 
		  HTTP_POST_TYPE contentType)
      throws JsonMappingException, HttpException, IOException
  {
	Preconditions.checkNotNull(url, "Url cannot be null");
	Preconditions.checkNotNull(verb, "HTTP Verb cannot be null");
	  
	String json = null;

	logger.debug("Calling url: " + url);
	
    RequestBody body = null;
    
    if (HTTP_POST_TYPE.FORM.equals(contentType) && input instanceof Map)
    {
    	FormBody.Builder formBuilder = new FormBody.Builder();
    	for (Map.Entry<String, String> entry : ((Map<String,String>)input).entrySet())
    	{
    		formBuilder.add(entry.getKey(), entry.getValue());
    	}
    	body = formBuilder.build();
    }
    else if (HTTP_POST_TYPE.URL.equals(contentType) && input instanceof Map) 
    {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
    	for (Map.Entry<String, String> entry : ((Map<String,String>)input).entrySet())
    	{
	        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
    	}
    	url = urlBuilder.toString();
    }
    else 
    {
    	if (input == null) 
    	{
    		json = "";
    	    logger.debug("Null payload");
    	} else if (input instanceof String) 
    	{
    		json = (String)input;
    	    logger.debug("Passed String rest object: [{}]", json);
    	} else
    	{
    	    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    	    json = ow.writeValueAsString(input);
    	    logger.debug("Converted rest object: {}", json);
    	}

    	body = RequestBody.create(JSON_MEDIA_TYPE, json);
    }

    okhttp3.Request.Builder builder = new Request.Builder();
    builder.url(url).addHeader("X-Atlassian-Token", "no-check").
    	addHeader("Accept", "application/json");
    
    if (HTTP_VERB.PUT.equals(verb))
    {
        builder = builder.put(body);
    } else if (HTTP_VERB.GET.equals(verb))
    {
    	builder = builder.get();
    } else if (HTTP_VERB.POST.equals(verb))
    {
    	builder = builder.post(body);
    } else if (HTTP_VERB.HEAD.equals(verb))
    {
    	builder = builder.head();
    }
    
    Response response = null;
    if (directAuth)
    {
    	String credentials =
			Credentials.basic(this.crowdUserDetailsService.
				loadUserByToken(crowdCookieValue).getUsername(),
				manager.getUserPassword());
    	builder = builder.addHeader("Authorization", credentials);
    	response = getClientFresh(crowdCookieValue).newCall(builder.build()).execute();
    }
    else 
    {
    	response = getClient(crowdCookieValue).newCall(builder.build()).execute();	
    }
    	    
    String respBody = response.body().string();
    response.close();
    logger.debug(url + " > " + response.code() + 
    	(respBody == null || respBody.trim().length() == 0 ?
    		"" : (": \n" + respBody  )) + " : " + verb);
    
    if (response.code() < 200 || response.code() >= 300)
    {
      throw new HttpException(response.code(), 
    	"Could not " + verb + " > "  + url + " : " + respBody);
    }

    if (returnType == null && returnTypeRef == null) 
    {
    	return null;
    } else if (returnType != null) 
    {
    	if (returnType.isAssignableFrom(String.class)) 
    	{
    		return (T)new String(respBody);
    	}
    	return (T)new ObjectMapper().readValue(respBody, returnType);
    } else
    {
    	return (T)new ObjectMapper().readValue(respBody, returnTypeRef);
    }
  }

  public void callHttpBasicFormAuthenticate(String url) throws IOException 
  {
	Preconditions.checkNotNull(url, "Url cannot be null");
	Preconditions.checkNotNull(SecurityContextHolder.getContext().getAuthentication(),
		"Cannot auth with null principal");
    CrowdUserDetails userDetails =
        (CrowdUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

//    String username = userDetails.getUsername();
//    String password = manager.getUserPassword();

    Map<String, String> params = new HashMap<>();
    params.put("j_username", userDetails.getUsername());
    params.put("j_password", manager.getUserPassword());
    
//    RequestBody body =
//        new FormBody.Builder().add("j_username", username).add("j_password", password).build();
//    Request request = new Request.Builder()
//        .url(url).post(body)
//        .build();
//    Response response = null;
//    try 
//    {
//    	response = getClient(null).newCall(request).execute();
//    	if (response.isSuccessful()) 
//    	{
//    		logger.debug("Successful form based auth");
//    	} else {
//    		throw new IOException("Could not authenticate: " + username + 
//    			" : " + response.body());
//    	}
//    }
//    finally {
//    	if (response != null)
//    		response.close();
//    }
     callHttpInternal(url, params, null, true, HTTP_VERB.POST, 
    	null, null, HTTP_POST_TYPE.FORM);
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
