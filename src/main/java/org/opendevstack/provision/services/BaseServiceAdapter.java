package org.opendevstack.provision.services;

import com.fasterxml.jackson.core.type.TypeReference;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.RestClient;
import org.opendevstack.provision.util.rest.Client;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class BaseServiceAdapter {

  final boolean useTechnicalUser;
  final String userName;
  final String userPassword;

  @Autowired
  IODSAuthnzAdapter manager;

  @Autowired  RestClient restClient;

  @Autowired
  Client client;

  public BaseServiceAdapter(String userName, String userPassword) {
    this.userName = userName;
    this.userPassword = userPassword;
    this.useTechnicalUser = !(isEmpty(userName) || isEmpty((userPassword)));
  }

  public <T> T callRestService(
      String url,
      Object input,
      RestClient.HTTP_VERB verb,
      Class returnType,
      TypeReference<T> returnTypeRef)
      throws IOException {

    if (useTechnicalUser) {
      T t =
          restClient.callHttpTypeRefWithDirectAuth(
              url, input, verb, returnType, returnTypeRef, userName, userPassword);
      return t;
    }

    return restClient.callHttpTypeRef(
        url, null, false, RestClient.HTTP_VERB.GET, new TypeReference<T>() {});
  }

  /**
   * Calls the RestService without any additional authentication, even if admin user is set or not.
   */
  public <T> T callHttpWithTypeWithoutAuthentication(
          String url,
          Object input,
          RestClient.HTTP_VERB verb,
          Class<T> returnType)
          throws IOException {

    return restClient.callHttp(
            url, input, false, verb, returnType);
  }

    public <T> T callHttpWithTypeRefWithoutAuthentication(
            String url,
            Object input,
            RestClient.HTTP_VERB verb,
            TypeReference<T> returnTypeRef)
            throws IOException {

        return restClient.callHttpTypeRef(
                url, input, false, verb, returnTypeRef);
    }
  public <T> T callHttp(String url, Object input, RestClient.HTTP_VERB verb, Class<T> returnType)
      throws IOException {
    if (useTechnicalUser) {
      return restClient.callHttpWithDirectAuth(
          url, input, RestClient.HTTP_VERB.POST, returnType, userName, userPassword);
    } else {
      return restClient.callHttp(url, input, false, RestClient.HTTP_VERB.POST, returnType);
    }
  }

  public void getSessionId(String url) throws IOException {
    if (useTechnicalUser) {
      return;
    }
    restClient.getSessionId(url);
  }

  public String getUserName() {
    return useTechnicalUser ? userName : manager.getUserName();
  }
  public void callHttpBasicFormAuthenticate(String url) throws IOException {
    if (useTechnicalUser) {
      restClient.callHttpBasicFormAuthenticate(url, userName, userPassword);
    } else {
      restClient.callHttpBasicFormAuthenticate(url);
    }
  }

  public CredentialsInfo getBasicCredentials() {
    return new CredentialsInfo(userName,userPassword);
  }
}
