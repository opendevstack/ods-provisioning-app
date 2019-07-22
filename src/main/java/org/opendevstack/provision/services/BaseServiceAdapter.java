package org.opendevstack.provision.services;

import static org.apache.commons.lang.StringUtils.isEmpty;

import javax.annotation.PostConstruct;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.RestClient.HTTP_VERB;
import org.opendevstack.provision.util.rest.Client;
import org.opendevstack.provision.util.rest.ClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class BaseServiceAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(BaseServiceAdapter.class);

  private boolean useTechnicalUser;
  protected String userName;
  protected String userPassword;
  private final String configurationPrefix;

  @Autowired IODSAuthnzAdapter manager;

  @Autowired private Environment environment;

  protected @Autowired Client client;

  public BaseServiceAdapter(String configurationPrefix) {
    this.configurationPrefix = configurationPrefix;
  }

  @PostConstruct
  public void afterPropertiesSet() {
    String logPrefix = String.format("Initialize bean %s -", getClass().getName());
    LOG.info("{} configuration prefix {}", logPrefix, configurationPrefix);

    String propertyAdminUserKey = configurationPrefix + ".admin_user";
    String propertyAdminUserPasswordKey = configurationPrefix + ".admin_password";

    this.userName = environment.getProperty(propertyAdminUserKey);
    this.userPassword = environment.getProperty(propertyAdminUserPasswordKey);

    this.useTechnicalUser = !isEmpty(userName) && !isEmpty(userPassword);
    if (useTechnicalUser) {
      LOG.info(
          "{} basic authentication via technical user is configured via property '{}'={}",
          logPrefix,
          propertyAdminUserKey,
          this.userName);
    } else {
      String reason =
          "property "
              + (isEmpty(userName) ? propertyAdminUserKey : propertyAdminUserPasswordKey)
              + " not defined";
      LOG.info(
          "{} basic authentication via logged in user is configured, since {}!", logPrefix, reason);
    }
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

    if (useTechnicalUser) {
      return call.basicAuthenticated(new CredentialsInfo(userName, userPassword));
    }
    CredentialsInfo credentialsInfo =
        new CredentialsInfo(manager.getUserName(), manager.getUserPassword());
    return call.basicAuthenticated(credentialsInfo);
  }

  public ClientCall notAuthenticatedCall(HTTP_VERB verb) {
    return client.call(verb);
  }
}
