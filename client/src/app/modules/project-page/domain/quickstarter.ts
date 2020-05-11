export interface QuickstarterData {
  id: string;
  enabled: boolean;
  name: string;
  description: string;
  gitRepoName: string;
  jenkinsfilePath: string;
  branch: string;
}

export interface ProjectQuickstarterComponentsData {
  id?: string;
  component_type: string;
  component_id: string;
  git_url_ssh: string;
  git_url_http: string;
  GROUP_ID: string;
  PROJECT_ID: string;
  PACKAGE_NAME: string;
  ODS_IMAGE_TAG: string;
  ODS_GIT_REF: string;
  component_description: string;
}

export interface ProjectQuickstarterComponent {
  id?: string;
  componentType: string;
  componentId: string;
  gitUrlSsh: string;
  gitUrlHttp: string;
  groupId: string;
  projectId: string;
  packageName: string;
  odsImageTag: string;
  odsGitRef: string;
  componentDescription: string;
}

export interface ProjectQuickstarter {
  description: string;
  type: string;
  ids: ProjectQuickstarterComponent[];
  isNewComponentPossible?: boolean;
}
