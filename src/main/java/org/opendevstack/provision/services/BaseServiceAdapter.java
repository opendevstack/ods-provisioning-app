package org.opendevstack.provision.services;

import static org.apache.commons.lang.StringUtils.isEmpty;

import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.opendevstack.provision.util.rest.Client;
import org.opendevstack.provision.util.rest.ClientCall;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseServiceAdapter {

  final boolean useTechnicalUser;
  final String userName;
  final String userPassword;

  @Autowired IODSAuthnzAdapter manager;

  private @Autowired Client client;

  public BaseServiceAdapter(String userName, String userPassword) {
    this.userName = userName;
    this.userPassword = userPassword;
    this.useTechnicalUser = !(isEmpty(userName) || isEmpty((userPassword)));
  }

  public String getUserName() {
    return useTechnicalUser ? userName : manager.getUserName();
  }

  public ClientCall httpGet() {
    return authenticatedCall(HTTP_VERB.GET);
  }

  public ClientCall httpPost() {
    return authenticatedCall(HTTP_VERB.POST);
  }

  public ClientCall httpPut() {
    return authenticatedCall(HTTP_VERB.PUT);
  }

  public ClientCall httpDelete() {
    return authenticatedCall(HTTP_VERB.DELETE);
  }

  private ClientCall authenticatedCall(HTTP_VERB verb) {
    ClientCall call = client.call(verb);
    if (useTechnicalUser) {
      return call.basicAuthenticated(getBasicCredentials());
    }
    throw new RuntimeException("Crowd or oauth-based authentication not yet implemented!");
  }

  private CredentialsInfo getBasicCredentials() {
    return new CredentialsInfo(userName, userPassword);
  }
}
