import { Directive, HostListener } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: '[appUppercase]'
})
export class UppercaseDirective {
  constructor(private host: NgControl) {}

  @HostListener('input', ['$event.target.value'])
  onInput = (value: string): void => {
    this.host.control.setValue(value.toUpperCase());
  };
}
