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

  public static nameExistsInProjectQuickstarterComponentsValidator(
    projectQuickstarters: ProjectQuickstarter[]
  ): ValidatorFn {
    return (control: FormControl): ValidationErrors | null => {
      const found = projectQuickstarters.some(
        (quickstarter: ProjectQuickstarter) => {
          return quickstarter.ids.some(
            (component: ProjectQuickstarterComponent) => {
              return component.id === control.value;
            }
          );
        }
      );

      return found
        ? {
            nameExistsInProjectQuickstarterComponents: true
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
      const existingComponentsForm = form.get(
        'existingComponentsForm'
      ) as FormGroup;
      const existingComponentsFormUserInputs = existingComponentsForm.controls
        .newComponent as FormArray;

      if (
        control.value === '' ||
        !Object.keys(newComponentsForm.controls).length ||
        !Object.keys(existingComponentsForm.controls).length
      ) {
        return null;
      }

      const found = [
        ...newComponentFormUserInputs.controls.map(
          (item: FormControl) => item.value.componentName
        ),
        ...existingComponentsFormUserInputs.controls.map(
          (item: FormControl) => item.value.componentName
        )
      ].some(item => item === control.value);

      return found
        ? {
            nameExistsInNewProjectQuickstarterComponents: true
          }
        : null;
    };
  }
}
