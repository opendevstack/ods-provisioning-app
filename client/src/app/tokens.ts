import { InjectionToken } from '@angular/core';

export const API_PROJECT_URL = new InjectionToken<string>('apiProjectUrl');
export const API_ALL_PROJECTS_URL = new InjectionToken<string>(
  'apiAllProjectsUrl'
);
export const API_GENERATE_PROJECT_KEY_URL = new InjectionToken<string>(
  'apiGenerateProjectKeyUrl'
);
export const API_ALL_QUICKSTARTERS_URL = new InjectionToken<string>(
  'apiAllQuickstartersUrl'
);
