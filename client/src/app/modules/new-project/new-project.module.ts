import { NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';
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
import { CustomErrorHandlerModule } from '../error-handler/custom-error-handler.module';

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
    CustomErrorHandlerModule,
    ProjectModule,
    LoadingIndicatorModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatButtonModule,
    NotificationModule,
    MatExpansionModule,
    ReactiveFormsModule,
    AppFormModule,
    MatCheckboxModule
  ],
  schemas: [NO_ERRORS_SCHEMA]
})
export class NewProjectModule {}
