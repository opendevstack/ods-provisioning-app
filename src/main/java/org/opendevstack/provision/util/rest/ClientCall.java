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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

public class ClientCall {

  private static final Logger logger = LoggerFactory.getLogger(ClientCall.class);

  private OkHttpClient client;
  private Request request = null;
  private String responseBody = null;

  private Request.Builder requestBuilder;

  // HTTP information
  private HttpMethod method = null;

  // Authentication
  private boolean isAuthenticated = false;
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
  private Map<String, String> header = new HashMap<>();;

  // Response information
  private Class returnType = null;
  private TypeReference typeReference = null;

  public ClientCall(OkHttpClient client) {
    this.client = client;
  }

  /**
   * Set HTTP method for execution
   *
   * @return ClientCall
   */
  public ClientCall method(HttpMethod method) {
    Preconditions.checkNotNull(method, "Method cannot be null");
    this.method = method;
    return this;
  }

  public ClientCall mediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public ClientCall json() {
    header.put("Accept", "application/json");
    return this;
  }

  public ClientCall request(Request request) {
    this.request = request;
    return this;
  }

  public ClientCall buildRequest() {
    this.requestBuilder = new Request.Builder();
    return this;
  }

  public ClientCall body(Object body) {
    Preconditions.checkNotNull(body, "Body cannot be null");
    this.body = body;
    return this;
  }

  public ClientCall returnType(Class returnType) {
    this.returnType = returnType;
    return this;
  }

  public ClientCall returnTypeReference(TypeReference typeReference) {
    this.typeReference = typeReference;
    return this;
  }

  public ClientCall url(String url) {
    this.url = url;
    return this;
  }

  public ClientCall authenticated() {
    this.isAuthenticated = true;
    return this;
  }

  public ClientCall basic() {
    Preconditions.checkArgument(this.isAuthenticated, "authenticated has to be set to use basic");
    isBasicAuth = true;
    return this;
  }

  public ClientCall credentials(CredentialsInfo credentialsInfo) {
    this.credentialsInfo = credentialsInfo;
    return this;
  }

  public ClientCall preAuthenticated() {
    this.isPreAuthenticated = true;
    return this;
  }

  public ClientCall queryParams(Map<String, String> params) {
    this.queryParams = params;
    return this;
  }

  public ClientCall preAuthUrl(String preAuthUrl) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated has to be set");
    this.preAuthUrl = preAuthUrl;
    return this;
  }

  public ClientCall preAuthContent(Object preAuthContent) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated() has to be set");
    Preconditions.checkArgument(this.preAuthUrl != null, "preAuthUrl() has to be set");
    this.preAuthContent = preAuthContent;
    return this;
  }

  public ClientCall header(Map<String, String> header) {
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

  private void prepareRequest() throws IOException {
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

  /** execute prepared call and deliver response */
  public <T> T execute() throws IOException {

    Preconditions.checkNotNull(this.client, "client cannot be null");
    Preconditions.checkNotNull(this.method, "HTTP method has to be set");

    if (request == null) {
      prepareRequest();
    }
    try {
      if (isPreAuthenticated) {
        logger.info("prepare preauthenticated call");
        Request preAuthRequest =
            (new Request.Builder()).url(preAuthUrl).post(prepareBody(preAuthContent)).build();
        try (Response preAuthResponse = this.client.newCall(preAuthRequest).execute()) {
          if (!preAuthResponse.isSuccessful()
              || preAuthResponse.body().string().contains("Invalid username and password")) {
            throw new IOException("Could not authenticate: " + preAuthResponse.body().string());
          }
          logger.info("Authenticated");
        }
      }
      try (Response callResponse = this.client.newCall(request).execute()) {
        String responseBody = callResponse.body().string();
        if (callResponse.code() < 200 || callResponse.code() >= 300) {
          throw new HttpException(
              callResponse.code(),
              "Could not " + request.method() + " > " + url + " : " + responseBody);
        }

        if (logger.isTraceEnabled()) {
          logger.trace(
              "URL: {}, method: {}, response-code: {}, responce-body: {} ",
              url,
              request.method(),
              callResponse.code(),
              "\n" + responseBody);
        } else {
          logger.debug(
              "URL: {}, method: {}, response-code: {}, responce-body: {} ",
              url,
              request.method(),
              callResponse.code(),
              "<body was omitted. Please enable tracing on class in order to see response body>");
        }
        this.responseBody = responseBody;
        return evaluateResponse();
      }
    } catch (IOException ex) {
      logger.error("Call failed: ", ex);
      throw ex;
    }
  }

  private <T> T evaluateResponse() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    if (returnType == null && typeReference == null) {
      return null;
    } else if (returnType != null) {
      if (returnType.isAssignableFrom(String.class)) {
        return (T) responseBody;
      }
      return (T) objectMapper.readValue(responseBody, returnType);
    } else {
      return (T) objectMapper.readValue(responseBody, typeReference);
    }
  }

  // TODO stefanlack/torsten What is the purpose of method authenticated().basic()
  public ClientCall basicAuthenticated(CredentialsInfo basicCredentials) {
    return this.authenticated().basic().credentials(basicCredentials);
  }
}
