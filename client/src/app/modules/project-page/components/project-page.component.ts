import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { EMPTY, forkJoin, Observable, of, Subject } from 'rxjs';
import { UpdateProjectQuickstartersData, UpdateProjectRequest, ProjectData, ProjectLink, ProjectStorage } from '../../../domain/project';
import { ProjectService } from '../../project/services/project.service';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { NotificationComponent } from '../../notification/components/notification.component';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';
import { ProjectQuickstarter, QuickstarterData } from '../../../domain/quickstarter';
import { AbstractControl, FormArray, FormBuilder } from '@angular/forms';
import { QuickstarterService } from '../services/quickstarter.service';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { StorageService } from '../../storage/services/storage.service';
import { HttpErrorTypes } from '../../http-interceptors/http-request-interceptor.service';
import { ConfirmationComponent } from '../../confirmation/components/confirmation.component';
import { ConfirmationConfig } from '../../confirmation/domain/confirmation-config';

type ProjectErrorType = 'NOT_FOUND' | 'NO_PROJECT_KEY' | 'UNKNOWN';

@Component({
  selector: 'app-project-page',
  templateUrl: './project-page.component.html',
  styleUrls: ['./project-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectPageComponent extends FormBaseComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();
  project$ = new Subject<ProjectData>();
  quickstarters$ = new Subject<ProjectData>();
  isLoading = true;
  isProjectError: boolean;
  isQuickstartersError: boolean;
  errorType: ProjectErrorType;
  project: ProjectData;
  projectLinks: ProjectLink[];
  aggregatedProjectLinks: string;
  allQuickstarters: QuickstarterData[];
  projectQuickstarters: ProjectQuickstarter[] = null;

  @Output() getEditModeFlag = new EventEmitter<boolean>();

  private static mapFormValuesToBackendModel(formArray: AbstractControl[]): UpdateProjectQuickstartersData[] {
    return formArray.map((component: FormArray) => {
      return {
        component_id: component.value.componentName,
        component_type: component.value.quickstarterType
      };
    });
  }

  constructor(
    public editMode: EditModeService,
    private route: ActivatedRoute,
    private formBuilder: FormBuilder,
    private projectService: ProjectService,
    private quickstarterService: QuickstarterService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
    private storageService: StorageService
  ) {
    super();
    this.editMode.context = 'edit';
  }

  ngOnInit() {
    let projectKey: string;
    const projectKeyFromStorage = this.getProjectKeyFromStorage();
    this.route.params.subscribe(param => {
      if (!param.key) {
        if (!projectKeyFromStorage) {
          this.switchToErrorDisplay('NO_PROJECT_KEY');
        }
        projectKey = projectKeyFromStorage;
      } else {
        projectKey = param.key;
      }
      this.loadProjectData(projectKey);
    });
  }

  loadProjectData(projectKey: string) {
    this.isLoading = true;
    this.cdr.detectChanges();
    this.initializeDataRetrieval(projectKey);
    this.initializeFormGroup();
  }

  openNotification(text: string, reload?: boolean) {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = text;
    const dialogRef = this.dialog.open(NotificationComponent, dialogConfig);
    if (reload) {
      dialogRef.afterClosed().subscribe(() => {
        window.location.href = document.querySelector('base').href;
      });
    }
  }

  intendRemoveProject() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = this.buildConfirmationConfig();
    dialogConfig.maxWidth = '600px';
    dialogConfig.width = '100';
    dialogConfig.panelClass = 'custom-dialog-panel';
    const dialogRef = this.dialog.open(ConfirmationComponent, dialogConfig);
    dialogRef.afterClosed().subscribe(submitRequest => {
      if (submitRequest) {
        this.editMode.enabled = false;
        this.isLoading = true;
        this.deleteProject();
      }
    });
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isProjectError;
  }

  intendFormSubmit(): void {
    this.submitButtonClicks++;
    if (this.form.valid) {
      this.saveProject();
    }
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private buildConfirmationConfig(): ConfirmationConfig {
    return {
      verify: {
        inputLabel: 'Project key',
        compareValue: this.project.projectKey
      },
      text: {
        title: 'Remove project',
        info:
          'This will delete your Jira- and Confluence spaces as well as Jenkins pipelines and OpenShift namespace' +
          ' (if existing). Bitbucket repositories will not be deleted.',
        ctaButtonLabel: 'Yes, remove project'
      }
    };
  }
  private getProjectKeyFromStorage(): string | null {
    const projectKeyFromStorage = this.storageService.getItem('project') as ProjectStorage;
    if (!projectKeyFromStorage) {
      return;
    }
    return projectKeyFromStorage.key;
  }

  private initializeDataRetrieval(projectKey: string) {
    forkJoin([this.getProjectByKey(projectKey), this.getAllQuickstarters()]).subscribe(([project, allQuickstarters]) => {
      this.project = project as ProjectData;
      this.prepareProjectLinks();

      this.projectQuickstarters = null;
      if (this.project.quickstarters.length) {
        this.projectQuickstarters = this.quickstarterService.transformProjectQuickstarterData(this.project.quickstarters);
      }

      this.storageService.saveItem('project', { key: this.project.projectKey });

      this.allQuickstarters = allQuickstarters as QuickstarterData[];
      this.isLoading = false;
      this.cdr.detectChanges();
    });
  }

  private saveProject() {
    const requestData: UpdateProjectRequest = this.createUpdateProjectRequestData();
    this.isLoading = true;
    return this.projectService.updateProject(requestData).subscribe(
      project => {
        this.openNotification(`${project.projectKey} successfully updated, reloading ...`, true);
      },
      () => {
        this.openNotification(`Project could not be updated, please try again soon`);
        this.isLoading = false;
        this.cdr.detectChanges();
        return EMPTY;
      }
    );
  }

  private deleteProject() {
    return this.projectService.deleteProject(this.project.projectKey).subscribe(
      () => {
        this.storageService.removeItem('project');
        this.openNotification(`${this.project.projectKey} successfully deleted, reloading ...`, true);
      },
      () => {
        this.openNotification(`Project could not be deleted, please try again soon`);
        this.isLoading = false;
        this.cdr.detectChanges();
        return EMPTY;
      }
    );
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      newComponentsForm: this.formBuilder.group({})
    });
  }

  private getProjectByKey(key: string): Observable<ProjectData> {
    if (this.project$) {
      this.project$.unsubscribe();
    }
    return this.projectService.getProjectByKey(key).pipe(
      takeUntil(this.destroy$),
      tap(() => (this.isProjectError = false)),
      catchError((errorType: HttpErrorTypes) => {
        this.storageService.removeItem('project');
        this.switchToErrorDisplay(errorType);
        return EMPTY;
      })
    );
  }

  private getAllQuickstarters(): Observable<boolean | QuickstarterData[]> {
    if (this.quickstarters$) {
      this.quickstarters$.unsubscribe();
    }
    return this.quickstarterService.getAllQuickstarters().pipe(
      takeUntil(this.destroy$),
      tap(() => (this.isQuickstartersError = false)),
      catchError(() => {
        this.isQuickstartersError = true;
        return of(false);
      })
    );
  }

  private prepareProjectLinks() {
    this.projectLinks = this.projectService.getProjectLinksConfig(this.project);
    this.aggregatedProjectLinks = this.projectService.getAggregateProjectLinks(this.projectLinks);
  }

  private switchToErrorDisplay(errorType: ProjectErrorType) {
    this.errorType = errorType;
    this.isProjectError = true;
    this.isLoading = false;
    this.cdr.detectChanges();
  }

  private createUpdateProjectRequestData(): UpdateProjectRequest {
    const newComponentsControls = (this.newComponentsForm.get('newComponent') as FormArray).controls;
    const allComponentsData = ProjectPageComponent.mapFormValuesToBackendModel(newComponentsControls);

    return {
      projectKey: this.project.projectKey,
      quickstarters: allComponentsData
    };
  }
}
