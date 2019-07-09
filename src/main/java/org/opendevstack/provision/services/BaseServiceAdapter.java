package org.opendevstack.provision.services;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.Credentials;
import org.opendevstack.provision.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class BaseServiceAdapter {

  final boolean useTechnicalUser;
  final String userName;
  final String userPassword;

  @Autowired RestClient client;

  public BaseServiceAdapter(String userName, String userPassword) {
    this.userName = userName;
    this.userPassword = userPassword;
    this.useTechnicalUser = !(isEmpty(userName) || isEmpty((userPassword)));
  }

  public <T> T callRestService(
      String url, Object input, RestClient.HTTP_VERB verb, Class returnType, TypeReference<T> returnTypeRef)
      throws IOException {

    if (useTechnicalUser) {
      T t = client.callHttpTypeRefWithDirectAuth(url, input, verb, returnType, returnTypeRef,userName,userPassword);
      return t;
    }

    return client.callHttpTypeRef(
        url, null, false, RestClient.HTTP_VERB.GET, new TypeReference<T>() {});
  }
}
