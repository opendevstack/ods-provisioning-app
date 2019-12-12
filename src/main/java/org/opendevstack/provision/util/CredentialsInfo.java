package org.opendevstack.provision.util;

import okhttp3.Credentials;
import org.opendevstack.provision.authentication.MissingCredentialsInfoException;

public class CredentialsInfo {
  private final String userName;
  private final String userPassword;

  public CredentialsInfo(String userName, String userPassword) {
    if (userName == null || userPassword == null) {
      throw new MissingCredentialsInfoException("Not able to create credentials info!");
    }

    this.userName = userName;
    this.userPassword = userPassword;
  }

  public String getCredentials() {
    return Credentials.basic(userName, userPassword);
  }
}
