package org.opendevstack.provision.util.rest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.net.ssl.*;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.opendevstack.provision.util.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestClient {

  private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

  @Value("${restClient.connect.timeout:30}")
  private int connectTimeout;

  @Value("${restClient.read.timeout:60}")
  private int readTimeout;

  @Value("${restClient.trust-all-certificates:false}")
  private boolean trustAllCertificates;

  private OkHttpClient client;

  @PostConstruct
  public void afterPropertiesSet() {

    if (trustAllCertificates) {
      LOG.warn(
          "Trust all certificates. Only set this property to true in development environment! [restClient.trust-all-certificates={}]",
          trustAllCertificates);
      client = TrustAllCertificatesClientFactory.createClient();
    } else {
      client = standardClient();
    }
  }

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

      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "method={}, url={} , body={}",
            request.method(),
            request.url(),
            request.body() != null ? bodyToString(request.body()) : "");
      }

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
      String preAuthResponseBody = "";
      try {
        preAuthResponseBody = preAuthResponse.body().string();
      } catch (Throwable t) {
        preAuthResponseBody = "could not read response body";
      }
      if (!preAuthResponse.isSuccessful()
          || preAuthResponseBody.contains("Invalid username and password")) {
        throw new IOException("Could not authenticate: " + preAuthResponseBody);
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

  public static String bodyToString(final RequestBody body) {
    try {
      if (null == body) {
        return null;
      }

      final Buffer buffer = new Buffer();
      body.writeTo(buffer);
      return buffer.readUtf8();
    } catch (final IOException e) {
      return "did not work";
    }
  }

  private static class TrustAllCertificatesClientFactory {

    public static OkHttpClient createClient() {

      try {
        final TrustManager[] trustAllCerts =
            new TrustManager[] {
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain, String authType) {}

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return new java.security.cert.X509Certificate[] {};
                }
              }
            };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier(
            new HostnameVerifier() {
              @Override
              public boolean verify(String hostname, SSLSession session) {
                return true;
              }
            });

        OkHttpClient httpClient = builder.build();
        return httpClient;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public OkHttpClient getClient() {
    return client;
  }
}
