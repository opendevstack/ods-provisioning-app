import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationComponent } from './components/notification.component';
import { MatDialogModule } from '@angular/material/dialog';

@NgModule({
  declarations: [NotificationComponent],
  imports: [CommonModule, MatDialogModule]
})
export class NotificationModule {}
