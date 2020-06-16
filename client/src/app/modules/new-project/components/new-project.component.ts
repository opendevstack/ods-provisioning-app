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
import { NewProjectRequest, ProjectTemplate } from '../../../domain/project';
import { ProjectService } from '../../project/services/project.service';
import { NewProjectValidators } from '../../app-form/validators/new-project.validators';
import { catchError, debounceTime, map } from 'rxjs/operators';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { NotificationComponent } from '../../notification/components/notification.component';
import { StorageService } from '../../storage/services/storage.service';
import { default as validationConfig } from '../../app-form/config/validation.json';

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
  validationConfig = validationConfig;

  constructor(
    private formBuilder: FormBuilder,
    private editMode: EditModeService,
    private router: Router,
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
    private storageService: StorageService
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
      this.createProject();
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
    if (!projectNameControl.valid || projectKeyControl.value !== '') {
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

  openDialog(text: string, reload?: boolean) {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = text;
    const dialogRef = this.dialog.open(NotificationComponent, dialogConfig);
    if (reload) {
      dialogRef.afterClosed().subscribe(() => {
        // workaround until https://jira.bix-digital.com/browse/PANFE-43 is done
        window.location.href =
          document.querySelector('base').href + 'index.html';
      });
    }
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      name: [
        '',
        [
          Validators.required,
          Validators.pattern(this.validationConfig.project.name.regex)
        ]
      ],
      key: [
        '',
        [
          Validators.required,
          Validators.pattern(this.validationConfig.project.key.regex)
        ]
      ],
      cdUser: null,
      template: [null, Validators.required],
      description: null,
      optInJira: null,
      permissionSet: this.formBuilder.group({
        admin: this.formBuilder.group(
          {
            name: null,
            group: null
          },
          {
            validators: [
              NewProjectValidators.isPermissionSetGroupMandatoryValidator
            ]
          }
        ),
        user: this.formBuilder.group(
          {
            name: null,
            group: null
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
        this.editMode.enabled = false;
        this.cdr.detectChanges();
        return EMPTY;
      }
    );
  }

  private mapFormValuesToBackendModel(): NewProjectRequest {
    return {
      projectName: this.form.get('name').value,
      projectKey: this.form.get('key').value,
      description: this.form.get('description').value,
      projectType: this.form.get('template').value,
      cdUser: this.form.get('cdUser').value,
      projectAdminUser: this.form.get('permissionSet').get('admin').get('name')
        .value,
      projectAdminGroup: this.form
        .get('permissionSet')
        .get('admin')
        .get('group').value,
      projectUserGroup: this.form.get('permissionSet').get('user').get('name')
        .value,
      projectReadonlyGroup: this.form
        .get('permissionSet')
        .get('user')
        .get('group').value,
      bugtrackerSpace: this.form.get('optInJira').value,
      platformRuntime: this.form.get('optInODS').value
    };
  }

  private createProject() {
    const requestData: NewProjectRequest = this.mapFormValuesToBackendModel();
    this.isLoading = true;
    return this.projectService.createProject(requestData).subscribe(
      project => {
        this.storageService.saveItem('project', { key: project.projectKey });
        this.openDialog(
          `${project.projectKey} successfully created, reloading ...`,
          true
        );
      },
      () => {
        this.openDialog(`Project could not be created, please try again soon`);
        this.isLoading = false;
        this.cdr.detectChanges();
        return EMPTY;
      }
    );
  }
}
