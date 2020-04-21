import { ModuleWithProviders, NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectPageComponent } from './components/project-page.component';
import { HttpClientModule } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ProjectService } from './services/project.service';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';
import { httpInterceptorProviders } from '../http-interceptors';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from './tokens';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../notification/notification.module';
import { RouterModule } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';

@NgModule({
  declarations: [ProjectPageComponent],
  imports: [
    CommonModule,
    RouterModule.forChild([{ path: '', component: ProjectPageComponent }]),
    HttpClientModule,
    LoadingIndicatorModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatTooltipModule,
    MatButtonModule,
    ClipboardModule,
    NotificationModule,
    MatExpansionModule
  ],
  providers: [ProjectService, httpInterceptorProviders],
  schemas: [NO_ERRORS_SCHEMA]
})
export class ProjectPageModule {
  static withOptions(options: {
    apiProjectUrl: string;
    apiAllProjectsUrl: string;
  }): ModuleWithProviders {
    return {
      ngModule: ProjectPageModule,
      providers: [
        {
          provide: API_PROJECT_URL,
          useValue: options.apiProjectUrl
        },
        {
          provide: API_ALL_PROJECTS_URL,
          useValue: options.apiAllProjectsUrl
        }
      ]
    };
  }
}
