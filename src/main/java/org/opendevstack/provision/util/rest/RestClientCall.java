package org.opendevstack.provision.util.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

public class RestClientCall {

  private static final Logger logger = LoggerFactory.getLogger(RestClientCall.class);

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  private Request request = null;
  private String responseBody = null;

  private Request.Builder requestBuilder;

  // HTTP information
  private HttpMethod method = null;

  // Authentication
  private boolean isBasicAuth = false;
  private CredentialsInfo credentialsInfo;

  // Pre authentication
  private boolean isPreAuthenticated = false;
  private String preAuthUrl = null;
  private Object preAuthContent;

  // Request information
  private MediaType mediaType;
  private Object body;
  private String url;
  private Map<String, String> queryParams;
  private Map<String, String> header = new HashMap<>();

  // Response information
  private Class returnType = null;
  private TypeReference returnTypeReference = null;

  private RestClientCall() { // prevent direct instantiation
  }

  public static RestClientCall get() {
    RestClientCall newCall = new RestClientCall();
    return newCall.buildRequest().method(HttpMethod.GET).json().mediaType(JSON_MEDIA_TYPE);
  }

  public static RestClientCall get(Map<String, String> queryParams) {
    return get().queryParams(queryParams);
  }

  public static RestClientCall post(Object body) {
    return post().body(body);
  }

  public static RestClientCall post() {
    RestClientCall newCall = new RestClientCall();
    return newCall.buildRequest().method(HttpMethod.POST).json().mediaType(JSON_MEDIA_TYPE);
  }

  public static RestClientCall delete() {
    RestClientCall newCall = new RestClientCall();
    return newCall.buildRequest().method(HttpMethod.DELETE).json().mediaType(JSON_MEDIA_TYPE);
  }

  public static RestClientCall put() {
    RestClientCall newCall = new RestClientCall();
    return newCall.buildRequest().method(HttpMethod.PUT).json().mediaType(JSON_MEDIA_TYPE);
  }

  public static RestClientCall put(Object body) {
    return put().body(body);
  }

  public static RestClientCall head() {
    RestClientCall newCall = new RestClientCall();
    return newCall.buildRequest().method(HttpMethod.HEAD).json().mediaType(JSON_MEDIA_TYPE);
  }

  public static RestClientCall call(HttpVerb verb) {
    switch (verb) {
      case PUT:
        return put();
      case GET:
        return get();
      case POST:
        return post();
      case HEAD:
        return head();
      case DELETE:
        return delete();
      default:
        return head();
    }
  }

  /**
   * Set HTTP method for execution
   *
   * @return ClientCall
   */
  private RestClientCall method(HttpMethod method) {
    Preconditions.checkNotNull(method, "Method cannot be null");
    this.method = method;
    return this;
  }

  public Request getRequest() {
    return request;
  }

  public RestClientCall mediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public RestClientCall json() {
    header.put("Accept", "application/json");
    return this;
  }

  public RestClientCall request(Request request) {
    this.request = request;
    return this;
  }

  public RestClientCall buildRequest() {
    this.requestBuilder = new Request.Builder();
    return this;
  }

  public RestClientCall body(Object body) {
    Preconditions.checkNotNull(body, "Body cannot be null");
    this.body = body;
    return this;
  }

  public RestClientCall returnType(Class returnType) {
    this.returnType = returnType;
    return this;
  }

  public RestClientCall returnTypeReference(TypeReference returnTypeReference) {
    this.returnTypeReference = returnTypeReference;
    return this;
  }

  public RestClientCall url(String url) {
    this.url = url;
    return this;
  }

  public RestClientCall url(String urlFormat, Object... args) {
    return url(String.format(urlFormat, args));
  }

  public RestClientCall credentials(CredentialsInfo credentialsInfo) {
    this.credentialsInfo = credentialsInfo;
    return this;
  }

  public RestClientCall preAuthenticated() {
    this.isPreAuthenticated = true;
    return this;
  }

  public RestClientCall queryParams(Map<String, String> params) {
    if (this.queryParams == null) {
      this.queryParams = params;
      return this;
    }
    this.queryParams.putAll(params);
    return this;
  }

  public RestClientCall queryParam(String paramName, String paramValue) {
    if (this.queryParams == null) {
      this.queryParams = new HashMap<>();
    }
    queryParams.put(paramName, paramValue);
    return this;
  }

  public RestClientCall preAuthUrl(String preAuthUrl) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated has to be set");
    this.preAuthUrl = preAuthUrl;
    return this;
  }

  public RestClientCall preAuthContent(Object preAuthContent) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated() has to be set");
    Preconditions.checkArgument(this.preAuthUrl != null, "preAuthUrl() has to be set");
    this.preAuthContent = preAuthContent;
    return this;
  }

  public RestClientCall header(Map<String, String> header) {
    this.header.putAll(header);
    return this;
  }

  private RequestBody prepareBody(Object body) throws JsonProcessingException {
    RequestBody requestBody = null;
    String json = "";

    if (body == null) {
      logger.debug("The request has no body");
    } else if (body instanceof String) {
      json = (String) body;
      logger.debug("Passed String rest object: [{}]", json);
      requestBody = RequestBody.create(this.mediaType, json);
    } else if (body instanceof Map) {
      Map<String, String> paramMap = ((Map) body);
      logger.debug("Passed parameter map, keys: [{}]", paramMap.keySet());
      FormBody.Builder form = new FormBody.Builder();
      for (Map.Entry<String, String> param : paramMap.entrySet()) {
        form.add(param.getKey(), param.getValue());
      }
      logger.debug("Created form {}", json);
      requestBody = form.build();
    } else {
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      json = ow.writeValueAsString(body);
      logger.debug("Converted rest object: {}", json);
      requestBody = RequestBody.create(this.mediaType, json);
    }

    return requestBody;
  }

  public Request getPreauthRequest() throws JsonProcessingException {
    Request preAuthRequest =
        (new Request.Builder()).url(preAuthUrl).post(prepareBody(preAuthContent)).build();
    return preAuthRequest;
  }

  private HttpUrl prepareUrl(String url, Map<String, String> params) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
    if (params != null) {
      for (Map.Entry<String, String> param : params.entrySet()) {
        urlBuilder.addQueryParameter(param.getKey(), param.getValue());
      }
    }
    HttpUrl preparedUrl = urlBuilder.build();
    logger.debug("Prepared URL [{}]", preparedUrl);
    return preparedUrl;
  }

  void prepareRequest() throws IOException {
    Preconditions.checkNotNull(this.method, "method cannot be null");
    if (requestBuilder == null) {
      this.buildRequest();
    }
    Request.Builder requestBuilder = this.requestBuilder;

    logger.debug("Prepare request for execution");

    requestBuilder.method(this.method.name(), prepareBody(this.body));
    requestBuilder.url(prepareUrl(this.url, this.queryParams));

    for (Map.Entry<String, String> param : this.header.entrySet()) {
      requestBuilder.addHeader(param.getKey(), param.getValue());
    }
    if (isBasicAuth) {
      requestBuilder.addHeader("Authorization", credentialsInfo.getCredentials());
    }

    this.request = requestBuilder.build();

    logger.debug("Prepared request: [{}]", request.toString());
  }

  public static Logger getLogger() {
    return logger;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public Builder getRequestBuilder() {
    return requestBuilder;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public boolean isBasicAuth() {
    return isBasicAuth;
  }

  public CredentialsInfo getCredentialsInfo() {
    return credentialsInfo;
  }

  public boolean isPreAuthenticated() {
    return isPreAuthenticated;
  }

  public String getPreAuthUrl() {
    return preAuthUrl;
  }

  public Object getPreAuthContent() {
    return preAuthContent;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public Object getBody() {
    return body;
  }

  public String getUrl() {
    return url;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public Map<String, String> getHeader() {
    return header;
  }

  public Class getReturnType() {
    return returnType;
  }

  public TypeReference getReturnTypeReference() {
    return returnTypeReference;
  }

  void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  <T> T evaluateResponse() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    if (returnType == null && returnTypeReference == null) {
      return null;
    } else if (returnType != null) {
      if (returnType.isAssignableFrom(String.class)) {
        return (T) responseBody;
      }
      return (T) objectMapper.readValue(responseBody, returnType);
    } else {
      return (T) objectMapper.readValue(responseBody, returnTypeReference);
    }
  }

  public RestClientCall basicAuthenticated(CredentialsInfo basicCredentials) {
    this.isBasicAuth = true;
    return credentials(basicCredentials);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("method", method)
        .append("isBasicAuth", isBasicAuth)
        .append("preAuthUrl", preAuthUrl)
        .append("body", body)
        .append("url", url)
        .append("queryParams", queryParams)
        .append("header", header)
        .append("returnType", returnType)
        .append("returnTypeReference", returnTypeReference)
        .toString();
  }
}
