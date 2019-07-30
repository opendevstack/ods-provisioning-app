package org.opendevstack.provision.util;

import okhttp3.Credentials;

public class CredentialsInfo {
  private final String userName;
  private final String userPassword;

  public CredentialsInfo(String userName, String userPassword) {
    this.userName = userName;
    this.userPassword = userPassword;
  }

  public String getCredentials() {
    return Credentials.basic(userName, userPassword);
  }
}
