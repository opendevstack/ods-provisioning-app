import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { Subject } from 'rxjs';
import { Project } from '../domain/project';
import { ProjectService } from '../services/project.service';
import { takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { NotificationComponent } from '../../notification/components/notification.component';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { BrowserService } from '../../browser/services/browser.service';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';

export enum ProjectLinkTypes {
  COLLABORATION_SPACE,
  CODE_REPOSITORY,
  PLATFORM_BUILD_ENGINE,
  PLATFORM_DEV_ENVIRONMENT,
  PLATFORM_TEST_ENVIRONMENT
}

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
  project: Project;
  projectLinksConfig: any;
  aggregatedProjectLinks: string;
  editMode = false;
  quickstarterTableSource: any;

  displayedColumns: string[] = ['component_description', 'component_id'];

  @Output() onGetEditModeFlag: EventEmitter<boolean> = new EventEmitter<
    boolean
  >();

  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private browserService: BrowserService,
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
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          this.project = response;

          /* TODO put into config file and read it from there */
          this.projectLinksConfig = [
            {
              type: ProjectLinkTypes.COLLABORATION_SPACE,
              url: this.project.collaborationSpaceUrl,
              iconName: 'jira',
              iconLabel: 'Jira / Confluence'
            },
            {
              type: ProjectLinkTypes.CODE_REPOSITORY,
              url: this.project.scmvcsUrl,
              iconName: 'bitbucket',
              iconLabel: 'Bitbucket'
            },
            {
              type: ProjectLinkTypes.PLATFORM_BUILD_ENGINE,
              url: this.project.platformBuildEngineUrl,
              iconName: 'jenkins',
              iconLabel: 'Jenkins'
            },
            {
              type: ProjectLinkTypes.PLATFORM_DEV_ENVIRONMENT,
              url: this.project.platformDevEnvironmentUrl,
              iconName: 'openshift',
              iconLabel: 'Openshift DEV'
            },
            {
              type: ProjectLinkTypes.PLATFORM_TEST_ENVIRONMENT,
              url: this.project.platformTestEnvironmentUrl,
              iconName: 'openshift',
              iconLabel: 'Openshift TEST'
            }
          ];

          this.aggregatedProjectLinks = this.getAggregateProjectLinks();
          this.isLoading = false;
          const dataSource = new MatTableDataSource(this.project.quickstarters);

          dataSource.sort = this.sort;
          this.quickstarterTableSource = this.project.quickstarters;

          this.cdr.detectChanges();
        },
        err => {
          console.log('fehler', err.message);
        }
      );
  }

  private getAggregateProjectLinks(): string | null {
    let aggregatedProjectLinks = '';
    if (!this.projectLinksConfig) {
      return null;
    }
    this.projectLinksConfig.map(projectLink => {
      if (projectLink.url) {
        aggregatedProjectLinks += projectLink.url + '\n';
      }
    });
    return aggregatedProjectLinks;
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
      this.browserService.scrollIntoViewById('new');
    }
  }

  deactivateEditMode() {
    if (this.editMode) {
      this.editMode = false;
      this.editModeService.emitEditModeFlag(this.editMode);
    }
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
