import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import { ProjectQuickstarter, QuickstarterData } from '../domain/quickstarter';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { EditProjectValidators } from '../../app-form/validators/edit-project.validators';

@Component({
  selector: 'project-quickstarter-list',
  templateUrl: './quickstarter-list.component.html',
  styleUrls: ['./quickstarter-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickstarterListComponent extends FormBaseComponent
  implements OnInit {
  @Input() editMode: boolean;
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Input() allQuickstarters: QuickstarterData[];
  @Input() form: FormGroup;
  @Output() onActivateEditMode = new EventEmitter<boolean>();

  constructor(
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit(): void {
    if (this.projectQuickstarters) {
      this.initializeFormGroup();
      this.projectQuickstarters = this.prepareProjectQuickstartersFormGroup();
    }
    this.cdr.detectChanges();
  }

  get newComponentArray(): FormArray {
    return this.form.get('newComponent') as FormArray;
  }

  getQuickstarterData(index: number) {
    const id = this.newComponentArray.controls[index].get('quickstarterId')
      .value;
    return this.projectQuickstarters.find(
      projectQuickstarter => id === projectQuickstarter.id
    );
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

  emitActivateEditMode() {
    this.onActivateEditMode.emit(true);
  }

  private initializeFormGroup(): void {
    this.form.addControl('newComponent', new FormArray([]));
  }

  private prepareProjectQuickstartersFormGroup(): ProjectQuickstarter[] {
    return this.projectQuickstarters.map((projectQuickstarter, index) => {
      let isNewComponentPossible = false;

      const newGroup = this.formBuilder.group({
        quickstarterId: [projectQuickstarter.id, [Validators.required]],
        componentName: ['']
      });

      if (this.allQuickstarters) {
        isNewComponentPossible = this.existsQuickstarterIdInAllQuickstarters(
          projectQuickstarter.id
        );

        newGroup
          .get('componentName')
          .setValidators([
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

  private existsQuickstarterIdInAllQuickstarters(searchId: string): boolean {
    return this.allQuickstarters.some(
      quickstarter => quickstarter.id === searchId
    );
  }
}
