package org.opendevstack.provision.util.rest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestClient {

  @Value("${restClient.connect.timeout:30}")
  int connectTimeout;

  @Value("${restClient.read.timeout:60}")
  int readTimeout;

  OkHttpClient client;

  @PostConstruct
  public void afterPropertiesSet() {
    client = standardClient();
  }

  private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

  public <T> T execute(RestClientCall call) throws IOException {

    if (call.getRequest() == null) {
      call.prepareRequest();
    }
    try {
      if (call.isPreAuthenticated()) {
        LOG.info("prepare preauthenticated call");
        Request preAuthRequest = call.getPreauthRequest();
        sendPreAuthRequest(preAuthRequest);
      }
      Request request = call.getRequest();
      try (Response callResponse = this.client.newCall(request).execute()) {
        String responseBody = callResponse.body().string();
        if (callResponse.code() < 200 || callResponse.code() >= 300) {
          throw new HttpException(
              callResponse.code(),
              "Could not " + request.method() + " > " + call.getUrl() + " : " + responseBody);
        }

        if (LOG.isTraceEnabled()) {
          LOG.trace(
              "URL: {}, method: {}, response-code: {}, responce-body: {} ",
              call.getUrl(),
              request.method(),
              callResponse.code(),
              "\n" + responseBody);
        } else {
          LOG.debug(
              "URL: {}, method: {}, response-code: {}, responce-body: {} ",
              call.getUrl(),
              request.method(),
              callResponse.code(),
              "<body was omitted. Please enable tracing on class in order to see response body>");
        }
        call.setResponseBody(responseBody);
        return call.evaluateResponse();
      }
    } catch (IOException ex) {
      LOG.error("Call failed: ", ex);
      throw ex;
    }
  }

  private void sendPreAuthRequest(Request preAuthRequest) throws IOException {

    try (Response preAuthResponse = this.client.newCall(preAuthRequest).execute()) {
      if (!preAuthResponse.isSuccessful()
          || preAuthResponse.body().string().contains("Invalid username and password")) {
        throw new IOException("Could not authenticate: " + preAuthResponse.body().string());
      }
      LOG.info("Authenticated");
    }
  }

  /** build a standard restClient */
  private OkHttpClient standardClient() {
    return configure().build();
  }

  private Builder configure() {
    return (new Builder())
        .cookieJar(new SimpleCookieJar())
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS);
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }
}
