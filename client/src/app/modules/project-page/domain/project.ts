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

export interface ProjectLink {
  url: string;
  iconName: string;
  iconLabel: string;
}

export enum ProjectErrorTypes {
  NO_PROJECT_KEY = 'NO_PROJECT_KEY'
}
