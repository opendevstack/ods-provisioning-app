package org.opendevstack.provision.util.rest;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.opendevstack.provision.util.CredentialsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;

public class ClientCall {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private OkHttpClient client;
  private Request request;
  private Response response;

  private Request.Builder requestBuilder;

  //HTTP information
  private HttpMethod method = null;

  //Authentication
  private boolean isAuthenticated = false;
  private boolean isBasicAuth = false;
  private CredentialsInfo credentialsInfo;

  //Pre authentication
  private boolean isPreAuthenticated = false;
  private String preAuthUrl = null;
  private Object preAuthContent;

  //Request information
  private MediaType mediaType;
  private Object content;
  private String url;

  //Response information
  private Class returnType = null;
  private TypeReference typeReference = null;


  public ClientCall(OkHttpClient client) {
    this.client = client;
  }

  /**
   * Set HTTP method for execution
   *
   * @param method
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

  public ClientCall request(Request request) {
    this.request = request;
    return this;
  }

  public ClientCall buildRequest() {
    this.requestBuilder = new Request.Builder();
    return this;
  }

  private Request.Builder getRequestBuilder() {
    if(this.requestBuilder == null) {
      buildRequest();
    }
    return this.requestBuilder;
  }

  public ClientCall content(Object content) {
    this.content = content;
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
    Preconditions.checkArgument(this.isAuthenticated, "authenticated() has to be called before using basic()");
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

  public ClientCall preAuthUrl(String preAuthUrl) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated() has to be called before applying preAuthUrl");
    this.preAuthUrl = preAuthUrl;
    return this;
  }

  public ClientCall preAuthContent(Object preAuthContent) {
    Preconditions.checkArgument(this.isPreAuthenticated, "preAuthenticated() has to be set before applying preAuthContent");
    Preconditions.checkArgument(this.preAuthUrl != null, "preAuthUrl() has to be set before applying preAuthContent");
    this.preAuthContent = preAuthContent;
    return this;
  }

  private String prepareRequestContent() {
    return null;
  }


  /**
   * execute prepared call and deliver response
   *
   * @param <T>
   * @return
   */
  public <T> T execute() throws IOException {
    Preconditions.checkNotNull(this.client, "client cannot be null");
    Preconditions.checkNotNull(this.method, "HTTP method has to be set");
    Preconditions.checkNotNull(this.request, "Request cannot be null");
    return null;
  }

}
