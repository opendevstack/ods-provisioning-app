package org.opendevstack.provision.services.jira;

public class JiraRestApi {

  public static final String JIRA_API_PROJECT = "project";
  public static final String JIRA_API_GROUPS_PICKER = "groups/picker";
  public static final String JIRA_API_USERS = "user";
  public static final String JIRA_API_ROLE = "role";
  public static final String JIRA_API_MYPERMISSIONS = "mypermissions";
  public static final String JIRA_API_PERMISSION_SCHEME = "permissionscheme";

  public static final String JIRA_API_BASE_PATTERN = "%s%s/";

  public static final String JIRA_API_USER_PATTERN = JIRA_API_BASE_PATTERN + JIRA_API_USERS;
  public static final String JIRA_API_GROUPS_PICKER_PATTERN =
      JIRA_API_BASE_PATTERN + JIRA_API_GROUPS_PICKER;

  public static final String JIRA_API_ROLE_PATTERN = JIRA_API_BASE_PATTERN + JIRA_API_ROLE + "/%s";
  public static final String JIRA_API_PROJECT_PATTERN = JIRA_API_BASE_PATTERN + JIRA_API_PROJECT;
  public static final String JIRA_API_PROJECT_FILTER_PATTERN = JIRA_API_PROJECT_PATTERN + "/%s";
  public static final String JIRA_API_PROJECT_PERMISSION_SCHEME_PATTERN =
      JIRA_API_PROJECT_PATTERN + "/%s/" + JIRA_API_PERMISSION_SCHEME;

  public static final String JIRA_API_PERMISSION_SCHEME_PATTERN =
      JIRA_API_BASE_PATTERN + JIRA_API_PERMISSION_SCHEME;
  public static final String JIRA_API_MYPERMISSIONS_PATTERN =
      JIRA_API_BASE_PATTERN + JIRA_API_MYPERMISSIONS;

  public static final String JIRA_API_PROJECT_ROLE_PATTERN =
      JIRA_API_PROJECT_PATTERN + "/%s/" + JIRA_API_ROLE + "/%s";
}
