package org.opendevstack.provision.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckPreconditionFailure {

  @JsonIgnore private final ExceptionCodes code;

  @JsonIgnore private final String detail;

  public enum ExceptionCodes {
    UNEXISTANT_GROUP,
    UNEXISTANT_USER,
    CREATE_PROJECT_PERMISSION_MISSING,
    GLOBAL_CREATE_PROJECT_BITBUCKET_PERMISSION_MISSING,
    EXCEPTION,
    PROJECT_EXISTS,
    PERMISSION_SCHEME_EXISTS,
    ROLE_EXISTS,
    ROLE_NOT_DEFINED;
  }

  public CheckPreconditionFailure(ExceptionCodes code, String detail) {
    this.code = code;
    this.detail = detail;
  }

  public static CheckPreconditionFailure getUnexistantGroupInstance(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.UNEXISTANT_GROUP, message);
  }

  public static CheckPreconditionFailure getExceptionInstance(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.EXCEPTION, message);
  }

  public static CheckPreconditionFailure getUnexistantUserInstance(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.UNEXISTANT_USER, message);
  }

  public static CheckPreconditionFailure getCreateProjectPermissionMissingInstance(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.UNEXISTANT_USER, message);
  }

  public static CheckPreconditionFailure getGlobalCreateProjectBitBucketPermissionMissingInstance(
      String message) {
    return new CheckPreconditionFailure(ExceptionCodes.UNEXISTANT_USER, message);
  }

  public static CheckPreconditionFailure getProjectExistsInstance(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.PROJECT_EXISTS, message);
  }

  public static CheckPreconditionFailure getUnexistantPermissionScheme(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.PERMISSION_SCHEME_EXISTS, message);
  }

  public static CheckPreconditionFailure getUnexistanRole(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.ROLE_EXISTS, message);
  }

  public static CheckPreconditionFailure getNotDefinedRole(String message) {
    return new CheckPreconditionFailure(ExceptionCodes.ROLE_NOT_DEFINED, message);
  }

  @Override
  public String toString() {
    return "CheckPreconditionFailure{"
        + "error-code='"
        + code
        + "', detail='"
        + detail
        + '\''
        + '}';
  }

  @JsonProperty("error-code")
  public String getCode() {
    return code.name();
  }

  @JsonProperty("error-message")
  public String getDetail() {
    return detail;
  }
}
