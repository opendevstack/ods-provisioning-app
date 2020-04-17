import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { Project, ProjectLink } from '../domain/project';
import { ProjectService } from '../services/project.service';
import { catchError, takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { NotificationComponent } from '../../notification/components/notification.component';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';
import { GroupedQuickstarters, Quickstarter } from '../domain/quickstarter';
import { default as projectLinksConfig } from '../config/project-links.conf.json';

@Component({
  selector: 'app-project-page',
  templateUrl: './project-page.component.html',
  styleUrls: ['./project-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectPageComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();
  project$ = new Subject<Project>();
  isLoading = true;
  isError: boolean;
  editMode = false;
  project: Project;
  projectLinks: ProjectLink[];
  aggregatedProjectLinks: string;
  quickstarters: GroupedQuickstarters[];

  @Output() onGetEditModeFlag: EventEmitter<boolean> = new EventEmitter<
    boolean
  >();

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private editModeService: EditModeService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.route.params.subscribe(param => {
      this.isLoading = true;
      this.cdr.detectChanges();
      if (this.project$) {
        this.project$.unsubscribe();
      }
      this.project$ = this.getProjectByKey(param.key);
    });
  }

  getProjectByKey(key: string): any {
    return this.projectService
      .getProjectByKey(key)
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => {
          this.isError = true;
          this.isLoading = false;
          this.cdr.detectChanges();
          return EMPTY;
        })
      )
      .subscribe((response: any) => {
        this.isError = false;
        this.isLoading = false;
        this.project = response;
        this.projectLinks = this.getProjectLinksConfig();
        this.aggregatedProjectLinks = this.getAggregateProjectLinks();

        if (this.project.quickstarters.length) {
          const groupedQuickstarters = this.groupQuickstartersByDescription(
            this.project.quickstarters
          );
          this.quickstarters = this.sortQuickstartersByDescription(
            groupedQuickstarters
          );
        }
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
    }
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isError;
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private getAggregateProjectLinks(): string | null {
    let aggregatedProjectLinks = '';
    if (!this.projectLinks) {
      return null;
    }
    this.projectLinks.map(projectLink => {
      if (projectLink.url) {
        aggregatedProjectLinks += projectLink.url + '\n';
      }
    });
    return aggregatedProjectLinks;
  }

  private groupQuickstartersByDescription(
    quickstarters: Quickstarter[]
  ): GroupedQuickstarters[] {
    return quickstarters.reduce((acc, quickstarter) => {
      const quickstarterObj: Quickstarter = {
        id: quickstarter.component_id,
        GROUP_ID: quickstarter.GROUP_ID,
        ODS_GIT_REF: quickstarter.ODS_GIT_REF,
        ODS_IMAGE_TAG: quickstarter.ODS_IMAGE_TAG,
        PACKAGE_NAME: quickstarter.PACKAGE_NAME,
        PROJECT_ID: quickstarter.PROJECT_ID,
        component_description: quickstarter.component_description,
        component_id: quickstarter.component_id,
        component_type: quickstarter.component_type,
        git_url_http: quickstarter.git_url_http,
        git_url_ssh: quickstarter.git_url_ssh
      };

      const found = acc.find(
        predicate => predicate.desc === quickstarter.component_description
      );

      if (found) {
        found.ids.push(quickstarterObj);
      } else {
        acc.push({
          desc: quickstarter.component_description,
          image_tag: quickstarter.ODS_IMAGE_TAG,
          ids: [quickstarterObj]
        });
      }
      return acc;
    }, []);
  }

  private sortQuickstartersByDescription(
    quickstarters: GroupedQuickstarters[]
  ): GroupedQuickstarters[] {
    return quickstarters.sort(
      (a: GroupedQuickstarters, b: GroupedQuickstarters) => {
        return a.desc.localeCompare(b.desc);
      }
    );
  }

  private getProjectLinksConfig(): ProjectLink[] {
    return projectLinksConfig.map(link => {
      return {
        url: this.project[link.urlKey],
        iconName: link.iconName,
        iconLabel: link.iconLabel
      };
    });
  }
}
