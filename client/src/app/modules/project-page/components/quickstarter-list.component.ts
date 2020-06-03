import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import {
  ProjectQuickstarter,
  QuickstarterData
} from '../../../domain/quickstarter';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { EditProjectValidators } from '../../app-form/validators/edit-project.validators';
import { Subject } from 'rxjs';
import { default as validationConfig } from '../../app-form/config/validation.json';
import { CustomValidation } from '../../app-form/domain/custom-validation';

@Component({
  selector: 'project-quickstarter-list',
  templateUrl: './quickstarter-list.component.html',
  styleUrls: ['./quickstarter-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickstarterListComponent extends FormBaseComponent
  implements OnInit, OnDestroy {
  @Input() editMode: boolean;
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Input() allQuickstarters: QuickstarterData[];
  @Input() form: FormGroup;
  @Output() onActivateEditMode = new EventEmitter<boolean>();
  componentNameCustomValidation: CustomValidation;
  destroy$ = new Subject<boolean>();

  constructor(
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit(): void {
    if (this.projectQuickstarters) {
      this.componentNameCustomValidation = this.getCustomValidationConfig(
        validationConfig.quickstarters.componentName
      );
      this.initializeFormGroup();
      this.projectQuickstarters = this.prepareProjectQuickstartersFormGroup();
    }
    this.cdr.detectChanges();
  }

  get newComponentArray(): FormArray {
    return this.form.get('newComponent') as FormArray;
  }

  getQuickstarterData(index: number) {
    const id = this.newComponentArray.controls[index].get('quickstarterType')
      .value;
    return this.projectQuickstarters.find(
      projectQuickstarter => id === projectQuickstarter.type
    );
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

  emitActivateEditMode() {
    this.onActivateEditMode.emit(true);
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private initializeFormGroup(): void {
    this.form.addControl('newComponent', new FormArray([]));
  }

  private prepareProjectQuickstartersFormGroup(): ProjectQuickstarter[] {
    return this.projectQuickstarters.map((projectQuickstarter, index) => {
      let isNewComponentPossible = false;

      const newGroup = this.formBuilder.group({
        quickstarterType: [projectQuickstarter.type, [Validators.required]],
        componentName: ['']
      });

      if (this.allQuickstarters) {
        isNewComponentPossible = this.existsQuickstarterTypeInAllQuickstarters(
          projectQuickstarter.type
        );

        newGroup
          .get('componentName')
          .setValidators([
            Validators.pattern(this.componentNameCustomValidation.regex),
            EditProjectValidators.nameExistsInProjectQuickstarterComponentsValidator(
              this.projectQuickstarters
            ),
            EditProjectValidators.nameExistsInAllQuickstartersValidator(
              this.allQuickstarters
            ),
            EditProjectValidators.nameExistsInNewProjectQuickstarterComponentsValidator(
              this.form.parent as FormGroup
            )
          ]);
      }

      this.newComponentArray.push(newGroup);

      return {
        ...projectQuickstarter,
        isNewComponentPossible
      };
    });
  }

  private existsQuickstarterTypeInAllQuickstarters(
    searchType: string
  ): boolean {
    return this.allQuickstarters.some(
      quickstarter => quickstarter.id === searchType
    );
  }
}
