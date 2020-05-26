import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from './services/project.service';
import { HttpClientModule } from '@angular/common/http';
import {
  API_ALL_PROJECTS_URL,
  API_GENERATE_PROJECT_KEY_URL,
  API_PROJECT_URL
} from '../../tokens';

@NgModule({
  declarations: [],
  imports: [CommonModule, HttpClientModule],
  providers: [ProjectService]
})
export class ProjectModule {
  static withOptions(options: {
    apiProjectUrl: string;
    apiAllProjectsUrl: string;
    apiGenerateProjectKeyUrl: string;
  }): ModuleWithProviders {
    return {
      ngModule: ProjectModule,
      providers: [
        {
          provide: API_PROJECT_URL,
          useValue: options.apiProjectUrl
        },
        {
          provide: API_ALL_PROJECTS_URL,
          useValue: options.apiAllProjectsUrl
        },
        {
          provide: API_GENERATE_PROJECT_KEY_URL,
          useValue: options.apiGenerateProjectKeyUrl
        }
      ]
    };
  }
}
