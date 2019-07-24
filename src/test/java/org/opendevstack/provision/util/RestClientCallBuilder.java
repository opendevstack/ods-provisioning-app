package org.opendevstack.provision.util;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.Request;
import org.opendevstack.provision.util.rest.RestClientCall;

public class RestClientCallBuilder extends RestClientCall {

  private List<String> methodCalled = new ArrayList<>();

  private RestClientCallBuilder() {}

  @Override
  public RestClientCall mediaType(MediaType mediaType) {
    addMethodCalled("mediaType");
    return super.mediaType(mediaType);
  }

  public void addMethodCalled(String methodName) {
    methodCalled.add(methodName);
  }

  @Override
  public RestClientCall json() {
    addMethodCalled("json");
    return super.json();
  }

  @Override
  public RestClientCall request(Request request) {
    addMethodCalled("request");
    return super.request(request);
  }

  @Override
  public RestClientCall body(Object body) {
    addMethodCalled("body");
    return super.body(body);
  }

  @Override
  public RestClientCall returnType(Class returnType) {
    addMethodCalled("returnType");
    return super.returnType(returnType);
  }

  @Override
  public RestClientCall returnTypeReference(TypeReference typeReference) {
    addMethodCalled("returnTypeReference");
    return super.returnTypeReference(typeReference);
  }

  @Override
  public RestClientCall url(String url) {
    addMethodCalled("url");
    return super.url(url);
  }

  @Override
  public RestClientCall credentials(CredentialsInfo credentialsInfo) {
    addMethodCalled("credentials");
    return super.credentials(credentialsInfo);
  }

  @Override
  public RestClientCall preAuthenticated() {
    addMethodCalled("preAuthenticated");
    return super.preAuthenticated();
  }

  @Override
  public RestClientCall queryParams(Map<String, String> params) {
    addMethodCalled("queryParams");
    return super.queryParams(params);
  }

  @Override
  public RestClientCall queryParam(String paramName, String paramValue) {
    addMethodCalled("queryParam");
    return super.queryParam(paramName, paramValue);
  }

  @Override
  public RestClientCall preAuthUrl(String preAuthUrl) {
    addMethodCalled("preAuthUrl");
    return super.preAuthUrl(preAuthUrl);
  }

  @Override
  public RestClientCall preAuthContent(Object preAuthContent) {
    addMethodCalled("preAuthContent");
    return super.preAuthContent(preAuthContent);
  }

  @Override
  public RestClientCall header(Map<String, String> header) {
    addMethodCalled("header");
    return super.header(header);
  }

  @Override
  public RestClientCall basicAuthenticated(CredentialsInfo basicCredentials) {
    addMethodCalled("basicAuthenticated");
    return super.basicAuthenticated(basicCredentials);
  }
}
