import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from './services/project.service';
import { HttpClientModule } from '@angular/common/http';
import {
  API_PROJECT_URL,
  API_GENERATE_PROJECT_KEY_URL,
  API_PROJECT_DETAIL_URL,
  API_PROJECT_TEMPLATES_URL
} from '../../tokens';

@NgModule({
  declarations: [],
  imports: [CommonModule, HttpClientModule],
  providers: [ProjectService]
})
export class ProjectModule {
  static withOptions(options: {
    apiProjectDetailUrl: string;
    apiProjectUrl: string;
    apiProjectTemplatesUrl: string;
    apiGenerateProjectKeyUrl: string;
  }): ModuleWithProviders<ProjectModule> {
    return {
      ngModule: ProjectModule,
      providers: [
        {
          provide: API_PROJECT_DETAIL_URL,
          useValue: options.apiProjectDetailUrl
        },
        {
          provide: API_PROJECT_URL,
          useValue: options.apiProjectUrl
        },
        {
          provide: API_PROJECT_TEMPLATES_URL,
          useValue: options.apiProjectTemplatesUrl
        },
        {
          provide: API_GENERATE_PROJECT_KEY_URL,
          useValue: options.apiGenerateProjectKeyUrl
        }
      ]
    };
  }
}
