import {Quickstarter} from "./quickstarter";

export interface Project {
  projectName: string;
  projectKey: string;
  description?: string;
  webhookProxySecret?: string;
  cdUser?: string;
  quickstarters?: Quickstarter[];
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
