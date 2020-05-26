import { NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { NotificationModule } from '../notification/notification.module';
import { RouterModule } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../app-form/app-form.module';
import { NewProjectComponent } from './components/new-project.component';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ProjectModule } from '../project/project.module';
import { httpInterceptorProviders } from '../http-interceptors';

@NgModule({
  declarations: [NewProjectComponent],
  imports: [
    CommonModule,
    RouterModule.forChild([
      {
        path: '',
        component: NewProjectComponent
      }
    ]),
    ProjectModule,
    LoadingIndicatorModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatTooltipModule,
    MatButtonModule,
    NotificationModule,
    MatExpansionModule,
    ReactiveFormsModule,
    AppFormModule,
    MatCheckboxModule
  ],
  providers: [httpInterceptorProviders],
  schemas: [NO_ERRORS_SCHEMA]
})
export class NewProjectModule {}
