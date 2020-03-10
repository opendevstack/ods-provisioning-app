import {Component, OnDestroy, OnInit, Output, EventEmitter, ViewChild} from '@angular/core';
import {Subject} from "rxjs";
import {Project} from "../domain/project";
import {ProjectService} from "../services/project.service";
import {delay, takeUntil} from "rxjs/operators";
import {ActivatedRoute} from "@angular/router";
import {MatDialog, MatDialogConfig} from "@angular/material/dialog";
import {NotificationComponent} from "../../notification/components/notification.component";
import {MatSort} from "@angular/material/sort";
import {MatTableDataSource} from "@angular/material/table";

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
  styleUrls: ['./project-page.component.scss']
})
export class ProjectPageComponent implements OnInit, OnDestroy {

  destroy$: Subject<boolean> = new Subject<boolean>();
  isLoading = true;
  project: Project;
  projectLinksConfig: any;
  aggregatedProjectLinks: string;
  editMode = false;
  quickstarterTableSource: any;
  displayedColumns: string[] = ['component_description', 'component_id'];
  @ViewChild(MatSort, {static: true}) sort: MatSort;


  constructor(private route: ActivatedRoute,
              private projectService: ProjectService,
              private dialog: MatDialog) { }

  ngOnInit(): void {
    this.getProjectFromUrl();
  }

  getProjectFromUrl(): any {

    const projectKey = this.route.snapshot.paramMap.get('key');

    return this.projectService.getProjectById(projectKey)
      .pipe(
        takeUntil(this.destroy$),
        delay(0)
      )
      .subscribe((response: any) => {
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

      }, err => {
        console.log('fehler', err.message);
      });
  }

  getAggregateProjectLinks(): string | null {
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

  openDialog(text: string): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = text;
    this.dialog.open(NotificationComponent, dialogConfig);
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
    // this.onEditModeChange.emit(this.editMode);
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }



}
