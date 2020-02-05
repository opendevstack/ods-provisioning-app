package org.opendevstack.provision.authentication;

public class MissingCredentialsInfoException extends RuntimeException {

  public MissingCredentialsInfoException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
