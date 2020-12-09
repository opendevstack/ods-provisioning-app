import { Directive, HostListener } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: '[appRemoveWhitespaces]'
})
export class RemoveWhitespacesDirective {
  constructor(private host: NgControl) {}

  @HostListener('blur', ['$event.target.value'])
  onBlur = (value: string) => {
    const cleanedValue = value.replace(/\s/g, '');
    if (cleanedValue !== value) {
      this.host.control.setValue(cleanedValue);
    }
  };
}
