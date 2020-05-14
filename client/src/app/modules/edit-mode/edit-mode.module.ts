import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EditMode } from './services/edit-mode.service';

@NgModule({
  imports: [CommonModule],
  providers: [EditMode]
})
export class EditModeModule {}
