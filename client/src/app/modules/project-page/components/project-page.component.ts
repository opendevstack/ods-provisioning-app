import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostListener,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import { EMPTY, forkJoin, Observable, of, Subject } from 'rxjs';
import { ProjectData, ProjectLink } from '../domain/project';
import { ProjectService } from '../services/project.service';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { NotificationComponent } from '../../notification/components/notification.component';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';
import { ProjectQuickstarter, QuickstarterData } from '../domain/quickstarter';
import { FormBuilder } from '@angular/forms';
import { QuickstarterService } from '../services/quickstarter.service';
import { FormBaseComponent } from '../../app-form/components/form-base.component';

@Component({
  selector: 'app-project-page',
  templateUrl: './project-page.component.html',
  styleUrls: ['./project-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectPageComponent extends FormBaseComponent
  implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();
  project$ = new Subject<ProjectData>();
  quickstarters$ = new Subject<ProjectData>();
  isLoading = true;
  isProjectError: boolean;
  isQuickstartersError: boolean;
  editMode = false;
  project: ProjectData;
  projectLinks: ProjectLink[];
  aggregatedProjectLinks: string;
  allQuickstarters: QuickstarterData[];
  projectQuickstarters: ProjectQuickstarter[] = null;

  @Output() onGetEditModeFlag = new EventEmitter<boolean>();

  constructor(
    private route: ActivatedRoute,
    private formBuilder: FormBuilder,
    private projectService: ProjectService,
    private quickstarterService: QuickstarterService,
    private editModeService: EditModeService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {
    super();
  }

  ngOnInit() {
    this.route.params.subscribe(param => {
      this.isLoading = true;
      this.cdr.detectChanges();
      this.initializeDataRetrieval(param.key);
      this.initializeFormGroup();
    });
  }

  initializeDataRetrieval(projectKey: string) {
    forkJoin([
      this.getProjectByKey(projectKey),
      this.getAllQuickstarters()
    ]).subscribe(([project, allQuickstarters]) => {
      this.project = project as ProjectData;
      this.prepareProjectLinks();

      if (this.project.quickstarters.length) {
        this.projectQuickstarters = this.quickstarterService.transformProjectQuickstarterData(
          this.project.quickstarters
        );
      }

      this.allQuickstarters = allQuickstarters as QuickstarterData[];
      this.isLoading = false;
      this.cdr.detectChanges();
    });
  }

  openDialog(text: string) {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = text;
    this.dialog.open(NotificationComponent, dialogConfig);
  }

  activateEditMode() {
    if (!this.editMode) {
      this.editMode = true;
      this.editModeService.emitEditModeFlag(this.editMode);
    }
  }

  deactivateEditMode() {
    if (this.editMode) {
      this.editMode = false;
      this.editModeService.emitEditModeFlag(this.editMode);
      this.cdr.detectChanges();
    }
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isProjectError;
  }

  intendFormSubmit(): void {
    this.submitButtonClicks++;
    if (this.form.valid) {
      this.saveProject();
      // initialize loading
      // Call ProjectService to send form data
      // show success or error
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      existingComponentsForm: this.formBuilder.group({}),
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
      catchError(() => {
        this.isProjectError = true;
        this.isLoading = false;
        this.cdr.detectChanges();
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
    this.aggregatedProjectLinks = this.projectService.getAggregateProjectLinks(
      this.projectLinks
    );
  }

  @HostListener('document:keydown', ['$event'])
  deactivateEditModeByEscKey(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      this.deactivateEditMode();
    }
  }

  saveProject() {
    // this.isLoading = true;
    console.log(this.project);
  }
}
