import { AbstractControl, FormGroup } from '@angular/forms';

export abstract class FormBaseComponent {
  form: FormGroup;
  submitButtonClicks = 0;

  protected constructor() {}

  get existingComponentsForm(): FormGroup {
    return this.form.get('existingComponentsForm') as FormGroup;
  }

  get newComponentsForm(): FormGroup {
    return this.form.get('newComponentsForm') as FormGroup;
  }

  hasValidationErrorByType(
    control: AbstractControl,
    errorType: string
  ): boolean {
    const hasTypeSpecificError = control.hasError(errorType);
    const hasInteractions = this.controlHasInteractions(control);

    if (!hasTypeSpecificError) {
      return false;
    }

    return hasTypeSpecificError && hasInteractions;
  }

  private controlHasInteractions(control: AbstractControl): boolean {
    return (
      control.touched ||
      control.dirty ||
      (control.untouched && control.value) ||
      this.submitButtonClicks > 0
    );
  }
}
