import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LogoutComponent } from './components/logout.component';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';

@NgModule({
  declarations: [LogoutComponent],
  imports: [
    CommonModule,
    RouterModule.forChild([
      {
        path: '',
        component: LogoutComponent
      }
    ]),
    MatButtonModule,
    MatIconModule,
    LoadingIndicatorModule
  ]
})
export class LogoutModule {}
