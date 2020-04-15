import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RemoveWhitespacesDirective } from './directives/remove-whitespaces.directive';

@NgModule({
  imports: [CommonModule],
  declarations: [RemoveWhitespacesDirective],
  exports: [RemoveWhitespacesDirective]
})
export class AppFormModule {}
