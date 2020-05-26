import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { FormBuilder, Validators } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';
import { Router } from '@angular/router';
import { ProjectTemplate } from '../../../domain/project';
import { ProjectService } from '../../project/services/project.service';
import { NewProjectValidators } from '../../app-form/validators/new-project.validators';
import { catchError, debounceTime, map } from 'rxjs/operators';

@Component({
  selector: 'app-new-project',
  templateUrl: './new-project.component.html',
  styleUrls: ['./new-project.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewProjectComponent extends FormBaseComponent
  implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();
  isLoading = true;
  isProjectTemplatesError: boolean;
  projectTemplates: ProjectTemplate[];

  constructor(
    private formBuilder: FormBuilder,
    private editMode: EditModeService,
    private router: Router,
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    this.editMode.context = 'new';
    this.editMode.enabled = true;
    this.initializeFormGroup();
    this.initializePage();
  }

  intendFormSubmit(): void {
    this.submitButtonClicks++;
    if (this.form.valid) {
      // initialize loading
      // Call ProjectService to send form data
      // show success or error
    }
  }

  leavePage() {
    this.editMode.enabled = false;
    this.router.navigateByUrl('/');
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isProjectTemplatesError;
  }

  generateProjectKey(): void | null {
    const projectNameControl = this.form.get('name');
    const projectKeyControl = this.form.get('key');
    if (projectNameControl.value === '' || projectKeyControl.value !== '') {
      return null;
    }

    projectKeyControl.disable();
    this.projectService
      .generateProjectKey(projectNameControl.value)
      .pipe(
        debounceTime(1000),
        map(result => result.projectKey),
        catchError(() => {
          projectKeyControl.enable();
          return EMPTY;
        })
      )
      .subscribe(projectKey => {
        projectKeyControl.setValue(projectKey);
        projectKeyControl.enable();
      });
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      name: ['', Validators.required],
      key: ['', Validators.required],
      cdUser: null,
      template: [null, Validators.required],
      description: ['', Validators.required],
      optInJira: null,
      optInODS: null,
      permissionSet: this.formBuilder.group({
        admin: this.formBuilder.group(
          {
            name: '',
            group: ''
          },
          {
            validators: [
              NewProjectValidators.isPermissionSetGroupMandatoryValidator
            ]
          }
        ),
        user: this.formBuilder.group(
          {
            name: '',
            group: ''
          },
          {
            validators: [
              NewProjectValidators.isPermissionSetGroupMandatoryValidator
            ]
          }
        )
      })
    });
  }

  private initializePage() {
    return this.projectService.getProjectTemplates().subscribe(
      projectTemplates => {
        this.projectTemplates = projectTemplates;
        this.isLoading = false;
        this.isProjectTemplatesError = false;
        this.cdr.detectChanges();
      },
      () => {
        this.isLoading = false;
        this.isProjectTemplatesError = true;
        this.cdr.detectChanges();
        return EMPTY;
      }
    );
  }
}
