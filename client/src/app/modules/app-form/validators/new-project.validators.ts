import { AbstractControl, ValidationErrors } from '@angular/forms';

export class NewProjectValidators {
  public static isPermissionSetGroupMandatoryValidator(
    control: AbstractControl
  ): ValidationErrors | null {
    const name = control.get('name');
    const group = control.get('group');

    if ((!!name.value && !!group.value) || (!name.value && !group.value)) {
      return null;
    }

    return {
      isPermissionSetGroupMandatory: true
    };
  }
}
