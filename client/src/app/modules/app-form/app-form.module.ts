import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RemoveWhitespacesDirective } from './directives/remove-whitespaces.directive';
import { UppercaseDirective } from './directives/uppercase.directive';

@NgModule({
  imports: [CommonModule],
  declarations: [UppercaseDirective, RemoveWhitespacesDirective],
  exports: [UppercaseDirective, RemoveWhitespacesDirective]
})
export class AppFormModule {}
