import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit
} from '@angular/core';
import {
  ProjectQuickstarter,
  QuickstarterData
} from '../../../domain/quickstarter';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EditProjectValidators } from '../../app-form/validators/edit-project.validators';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { Subject } from 'rxjs';
import { default as validationConfig } from '../../app-form/config/validation.json';
import { CustomValidation } from '../../app-form/domain/custom-validation';

@Component({
  selector: 'app-project-quickstarter-add',
  templateUrl: './quickstarter-add.component.html',
  styleUrls: ['./quickstarter-add.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickstarterAddComponent extends FormBaseComponent
  implements OnInit, OnDestroy {
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Input() allQuickstarters: QuickstarterData[];
  @Input() form: FormGroup;
  componentNameCustomValidation: CustomValidation;
  destroy$ = new Subject<boolean>();

  constructor(
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    this.componentNameCustomValidation = this.getCustomValidationConfig(
      validationConfig.quickstarters.componentName
    );
    this.initializeFormGroup();
  }

  get newComponentArray(): FormArray {
    return this.form.get('newComponent') as FormArray;
  }

  addInput() {
    if (!this.newComponentArray.valid) {
      return;
    }
    this.newComponentArray.push(this.createNewFormGroup());
    this.cdr.detectChanges();
  }
  removeInput(index: number) {
    if (this.removeAllowed()) {
      this.newComponentArray.removeAt(index);
      this.cdr.detectChanges();
    }
  }

  removeAllowed(): boolean {
    return this.newComponentArray.length > 1;
  }

  controlHasErrorByType(
    index,
    controlName: string,
    errorType: string
  ): boolean {
    return this.hasErrorByType(
      this.newComponentArray.controls[index].get(controlName),
      errorType
    );
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private initializeFormGroup(): void {
    this.form.addControl(
      'newComponent',
      new FormArray([this.createNewFormGroup()])
    );
  }

  private createNewFormGroup(): FormGroup {
    return this.formBuilder.group({
      quickstarterType: ['', [Validators.required]],
      componentName: [
        '',
        [
          Validators.required,
          Validators.pattern(this.componentNameCustomValidation.regex),
          EditProjectValidators.nameExistsInAllQuickstartersValidator(
            this.allQuickstarters
          ),
          EditProjectValidators.nameExistsInNewProjectQuickstarterComponentsValidator(
            this.form.parent as FormGroup
          )
        ]
      ]
    });
  }
}
