import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GeneralErrorPageComponent } from './components/general-error-page.component';

@NgModule({
  declarations: [GeneralErrorPageComponent],
  imports: [CommonModule],
  exports: [GeneralErrorPageComponent]
})
export class GeneralErrorPageModule {}
