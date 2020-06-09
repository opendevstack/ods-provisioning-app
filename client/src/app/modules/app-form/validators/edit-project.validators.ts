import {
  FormArray,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn
} from '@angular/forms';
import {
  ProjectQuickstarter,
  ProjectQuickstarterComponent,
  QuickstarterData
} from '../../../domain/quickstarter';

export class EditProjectValidators {
  public static nameExistsInAllQuickstartersValidator(
    allQuickstarters: QuickstarterData[]
  ): ValidatorFn {
    return (control: FormControl): ValidationErrors | null => {
      const found = allQuickstarters.some((quickstarter: QuickstarterData) => {
        return quickstarter.id === control.value;
      });

      return found
        ? {
            nameExistsInAllQuickstarters: true
          }
        : null;
    };
  }

  public static nameExistsInNewProjectQuickstarterComponentsValidator(
    form: FormGroup
  ): ValidatorFn {
    return (control: FormControl): ValidationErrors | null => {
      const newComponentsForm = form.get('newComponentsForm') as FormGroup;
      const newComponentFormUserInputs = newComponentsForm.controls
        .newComponent as FormArray;

      if (
        control.value === '' ||
        !Object.keys(newComponentsForm.controls).length
      ) {
        return null;
      }

      const found = newComponentFormUserInputs.controls
        .map((item: FormControl) => item.value.componentName)
        .some(item => item === control.value);

      return found
        ? {
            nameExistsInNewProjectQuickstarterComponents: true
          }
        : null;
    };
  }
}
