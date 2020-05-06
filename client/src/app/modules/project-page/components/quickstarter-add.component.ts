import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit
} from '@angular/core';
import { ProjectQuickstarter, QuickstarterData } from '../domain/quickstarter';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EditProjectValidators } from '../../app-form/validators/edit-project.validators';
import { FormBaseComponent } from '../../app-form/components/form-base.component';

@Component({
  selector: 'project-quickstarter-add',
  templateUrl: './quickstarter-add.component.html',
  styleUrls: ['./quickstarter-add.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickstarterAddComponent extends FormBaseComponent
  implements OnInit {
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Input() allQuickstarters: QuickstarterData[];
  @Input() form: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
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

  quickstarterHasValidationErrorByType(
    index,
    controlName: string,
    errorType: string
  ): boolean {
    return this.hasValidationErrorByType(
      this.newComponentArray.controls[index].get(controlName),
      errorType
    );
  }

  private initializeFormGroup(): void {
    this.form.addControl(
      'newComponent',
      new FormArray([this.createNewFormGroup()])
    );
  }

  private createNewFormGroup(): FormGroup {
    return this.formBuilder.group({
      quickstarterId: ['', [Validators.required]],
      componentName: [
        '',
        [
          Validators.required,
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
