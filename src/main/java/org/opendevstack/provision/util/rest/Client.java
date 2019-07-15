package org.opendevstack.provision.util.rest;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction to handle the OkHTTP client and use it in the same way from different classes to
 * prevent redundant code.
 *
 * @author Torsten Jaeschke
 */
@Component
public class Client {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  @Value("${client.connect.timeout:30}")
  int connectTimeout;

  @Value("${client.read.timeout:60}")
  int readTimeout;

  /**
   * Build a custom call to another API with a standard client
   *
   * @return ClientCall call to execute and to get response.
   */
  public ClientCall newCall() {
    return new ClientCall(standardClient());
  }

  /**
   * Build a custom call with a passed client
   */
  public ClientCall newCall(OkHttpClient client) {
    return new ClientCall(client);
  }

  /**
   * build a standard client
   */
  private OkHttpClient standardClient() {
    return configure().build();
  }

  public Builder configure() {
    return (new Builder())
        .cookieJar(new SimpleCookieJar())
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS);
  }

  public ClientCall get() {
    return newCall()
        .buildRequest()
        .method(HttpMethod.GET)
        .json()
        .mediaType(JSON_MEDIA_TYPE);
  }

  public ClientCall get(Map<String, String> queryParams) {
    return newCall()
        .buildRequest()
        .method(HttpMethod.GET)
        .json()
        .queryParams(queryParams)
        .mediaType(JSON_MEDIA_TYPE);
  }

  public ClientCall post() {
    return newCall()
        .buildRequest()
        .method(HttpMethod.POST)
        .json()
        .mediaType(JSON_MEDIA_TYPE);
  }

  public ClientCall post(Object body) {
    return newCall()
        .buildRequest()
        .method(HttpMethod.POST)
        .body(body)
        .json()
        .mediaType(JSON_MEDIA_TYPE);
  }

  public ClientCall put() {
    return newCall()
        .buildRequest()
        .method(HttpMethod.PUT)
        .json()
        .mediaType(JSON_MEDIA_TYPE);

  }

  public ClientCall put(Object body) {
    return newCall()
        .buildRequest()
        .method(HttpMethod.PUT)
        .body(body)
        .json()
        .mediaType(JSON_MEDIA_TYPE);

  }

  public ClientCall head() {
    return newCall()
        .buildRequest()
        .method(HttpMethod.HEAD)
        .json()
        .mediaType(JSON_MEDIA_TYPE);
  }
}
