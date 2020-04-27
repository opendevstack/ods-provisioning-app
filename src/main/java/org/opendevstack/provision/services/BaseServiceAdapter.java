package org.opendevstack.provision.services;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.annotation.PostConstruct;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.authentication.MissingCredentialsInfoException;
import org.opendevstack.provision.util.CredentialsInfo;
import org.opendevstack.provision.util.HttpVerb;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class BaseServiceAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(BaseServiceAdapter.class);

  boolean useTechnicalUser;
  protected String userName;
  protected String userPassword;
  private final String configurationPrefix;

  @Autowired IODSAuthnzAdapter manager;

  @Autowired private Environment environment;

  @Autowired private RestClient restClient;

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

  public String getUserPassword() {
    return useTechnicalUser ? userPassword : manager.getUserPassword();
  }

  public RestClientCall httpGet() {
    return authenticatedCall(HttpVerb.GET);
  }

  public RestClientCall httpPost() {
    return authenticatedCall(HttpVerb.POST);
  }

  public RestClientCall httpPut() {
    return authenticatedCall(HttpVerb.PUT);
  }

  public RestClientCall httpDelete() {
    return authenticatedCall(HttpVerb.DELETE);
  }

  private RestClientCall authenticatedCall(HttpVerb verb) {
    RestClientCall call = notAuthenticatedCall(verb);

    try {
      if (useTechnicalUser) {
        return call.basicAuthenticated(new CredentialsInfo(userName, userPassword));
      }
      CredentialsInfo credentialsInfo =
          new CredentialsInfo(manager.getUserName(), manager.getUserPassword());
      return call.basicAuthenticated(credentialsInfo);
    } catch (IllegalArgumentException ex) {
      throw new MissingCredentialsInfoException("Not able to create credentials info!", ex);
    }
  }

  public RestClientCall notAuthenticatedCall(HttpVerb verb) {
    return RestClientCall.call(verb);
  }

  public void setRestClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public RestClient getRestClient() {
    return restClient;
  }
}
