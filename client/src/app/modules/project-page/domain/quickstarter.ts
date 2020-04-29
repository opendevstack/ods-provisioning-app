export interface Quickstarter {
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

export interface GroupedQuickstarters {
  desc: string;
  ids: Quickstarter[];
}
