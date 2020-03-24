import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EditModeService } from './services/edit-mode.service';

@NgModule({
  imports: [CommonModule],
  providers: [EditModeService]
})
export class EditModeModule {}
