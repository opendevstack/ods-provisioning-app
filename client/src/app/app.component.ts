import { ChangeDetectorRef, Component, OnInit, Renderer2 } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { EditModeService } from './modules/edit-mode/services/edit-mode.service';
import { catchError, filter } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { EditModeFlag } from './modules/edit-mode/domain/edit-mode';
import { StorageService } from './modules/storage/services/storage.service';
import { NavigationStart, Router } from '@angular/router';
import { ProjectService } from './modules/project/services/project.service';
import { ProjectData, ProjectStorage } from './domain/project';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoading = true;
  isError: boolean;
  isNewProjectFormActive = false;

  projects: ProjectData[] = [];

  constructor(
    public editMode: EditModeService,
    public router: Router,
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer,
    private renderer: Renderer2,
    private projectService: ProjectService,
    private storageService: StorageService
  ) {
    this.matIconRegistry.addSvgIconSet(
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        'assets/icons/mdi-custom-icons.svg'
      )
    );
    this.matIconRegistry.addSvgIconSet(
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        'assets/icons/bi-stack.svg'
      )
    );
  }

  ngOnInit() {
    this.checkRedirectToProjectDetail();
    this.loadAllProjects();
  }

  getEditModeStatus() {
    this.editMode.onGetEditModeFlag.subscribe((editMode: EditModeFlag) => {
      if (editMode.enabled) {
        this.renderer.addClass(document.body, 'status-editmode-active');
        if (editMode.context === 'new') {
          this.isNewProjectFormActive = true;
        }
      } else {
        this.renderer.removeClass(document.body, 'status-editmode-active');
        this.isNewProjectFormActive = false;
      }
    });
  }

  private checkRedirectToProjectDetail() {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationStart && event.url === '/')
      )
      .subscribe(() => {
        const projectKey = this.getProjectKeyFormStorage();
        if (projectKey) {
          this.router.navigateByUrl(`/project/${projectKey}`);
        }
      });
  }

  private getProjectKeyFormStorage(): string | undefined {
    const projectKeyFromStorage = this.storageService.getItem(
      'project'
    ) as ProjectStorage;
    return projectKeyFromStorage?.key;
  }

  private loadAllProjects() {
    this.projectService
      .getAllProjects()
      .pipe(
        catchError(() => {
          this.isError = true;
          this.isLoading = false;
          return EMPTY;
        })
      )
      .subscribe((response: ProjectData[]) => {
        this.projects = response;
        this.isLoading = false;
        this.isError = false;
      });
  }
}
