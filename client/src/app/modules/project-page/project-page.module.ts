import { ModuleWithProviders, NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectPageComponent } from './components/project-page.component';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';
import { API_ALL_QUICKSTARTERS_URL } from '../../tokens';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../notification/notification.module';
import { RouterModule } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../app-form/app-form.module';
import { ProjectHeaderComponent } from './components/header.component';
import { QuickstarterListComponent } from './components/quickstarter-list.component';
import { QuickstarterAddComponent } from './components/quickstarter-add.component';
import { httpInterceptorProviders } from '../http-interceptors';
import { ProjectModule } from '../project/project.module';
import { ConfirmationModule } from '../confirmation/confirmation.module';

@NgModule({
  declarations: [
    ProjectPageComponent,
    ProjectHeaderComponent,
    QuickstarterListComponent,
    QuickstarterAddComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([
      {
        path: '',
        component: ProjectPageComponent
      }
    ]),
    ProjectModule,
    LoadingIndicatorModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatButtonModule,
    ClipboardModule,
    NotificationModule,
    ConfirmationModule,
    MatExpansionModule,
    ReactiveFormsModule,
    AppFormModule
  ],
  providers: [httpInterceptorProviders],
  schemas: [NO_ERRORS_SCHEMA]
})
export class ProjectPageModule {
  static withOptions(options: {
    apiAllQuickstartersUrl: string;
  }): ModuleWithProviders {
    return {
      ngModule: ProjectPageModule,
      providers: [
        {
          provide: API_ALL_QUICKSTARTERS_URL,
          useValue: options.apiAllQuickstartersUrl
        }
      ]
    };
  }
}
