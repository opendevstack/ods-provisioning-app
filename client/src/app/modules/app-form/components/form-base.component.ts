import { AbstractControl, FormGroup } from '@angular/forms';
import { CustomValidation } from '../domain/custom-validation';

export abstract class FormBaseComponent {
  form: FormGroup;
  submitButtonClicks = 0;

  protected constructor() {}

  get newComponentsForm(): FormGroup {
    return this.form.get('newComponentsForm') as FormGroup;
  }

  hasErrorByType(control: AbstractControl, errorType: string): boolean {
    const hasTypeSpecificError = control.hasError(errorType);
    const hasInteractions = this.controlHasInteractions(control);

    if (!hasTypeSpecificError) {
      return false;
    }

    return hasTypeSpecificError && hasInteractions;
  }

  getCustomValidationConfig(validationConfig): CustomValidation {
    return {
      regex: validationConfig.regex,
      errorMessages: validationConfig.errorMessages
    };
  }

  private controlHasInteractions(control: AbstractControl): boolean {
    return control.touched || control.dirty || (control.untouched && control.value) || this.submitButtonClicks > 0;
  }
}
