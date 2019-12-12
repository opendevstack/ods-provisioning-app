package org.opendevstack.provision.util;

import okhttp3.Credentials;
import org.springframework.util.Assert;

public class CredentialsInfo {
  private final String userName;
  private final String userPassword;

  public CredentialsInfo(String userName, String userPassword) {
    Assert.notNull(userName, "userName is null!");
    Assert.notNull(userPassword, "userPassword is null!");
    this.userName = userName;
    this.userPassword = userPassword;
  }

  public String getCredentials() {
    return Credentials.basic(userName, userPassword);
  }
}
