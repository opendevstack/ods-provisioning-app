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

  protected @Autowired Client client;

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
    ClientCall call = notAuthenticatedCall(verb);

    // TODO stefanlack: configure switch between technical user credentials from
    // SessionAwarePasswordHolder in application proerty
    //  --> make it explicit via property
    if (useTechnicalUser) {
      return call.basicAuthenticated(getBasicTechnicalUserCredentials());
    }
    CredentialsInfo credentialsInfo =
        new CredentialsInfo(manager.getUserName(), manager.getUserPassword());
    return call.basicAuthenticated(credentialsInfo);
  }

  public ClientCall notAuthenticatedCall(HTTP_VERB verb) {
    return client.call(verb);
  }

  private CredentialsInfo getBasicTechnicalUserCredentials() {
    return new CredentialsInfo(userName, userPassword);
  }
}
