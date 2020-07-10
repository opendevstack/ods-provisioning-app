import { ProjectQuickstarterComponentsData } from './quickstarter';

/* TODO
   Changing the current prov-app API is not in scope now, so here I use the same model from the backend.
   This should be improved there as well in the future.
*/
export interface ProjectData {
  projectName: string;
  projectKey: string;
  description?: string;
  webhookProxySecret?: string;
  cdUser?: string;
  quickstarters?: ProjectQuickstarterComponentsData[];
  bugtrackerSpace: boolean;
  platformRuntime: boolean;
  bugtrackerUrl?: string;
  collaborationSpaceUrl?: string;
  scmvcsUrl: string;
  repositories: any;
  platformBuildEngineUrl: string;
  platformCdEnvironmentUrl?: string;
  platformDevEnvironmentUrl?: string;
  platformTestEnvironmentUrl?: string;
  projectAdminGroup?: string;
  projectUserGroup?: string;
  projectAdminUser?: string;
  projectReadonlyGroup?: string;
  specialPermissionSet: boolean;
  lastExecutionJobs: any;
  projectType?: string;
  physicalLocation: string;
}

export interface UpdateProjectRequest {
  projectKey: string;
  quickstarters: UpdateProjectQuickstartersData[];
}

export interface DeleteComponentRequest {
  projectKey: string;
  quickstarters: UpdateProjectQuickstartersData[];
}

export interface UpdateProjectQuickstartersData {
  component_type: string;
  component_id: string;
}

export interface ProjectLink {
  url: string;
  iconName: string;
  iconLabel: string;
}

export interface ProjectStorage {
  key: string;
}

export interface NewProjectRequest {
  projectName: string;
  projectKey: string;
  description: string;
  projectType: string;
  cdUser: string;
  projectAdminUser: string;
  projectAdminGroup: string;
  projectUserGroup: string;
  projectReadonlyGroup: string;
  bugtrackerSpace: boolean;
  platformRuntime: boolean;
}

export interface ProjectKeyResponse {
  projectKey: string;
}

export interface ProjectTemplate {
  name: string;
  key: string;
}
