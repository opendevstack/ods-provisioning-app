import { Component, OnInit, Renderer2 } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { EditModeService } from './modules/edit-mode/services/edit-mode.service';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { EditModeFlag } from './modules/edit-mode/domain/edit-mode';
import { StorageService } from './modules/storage/services/storage.service';
import { NavigationStart, Router } from '@angular/router';
import { ProjectService } from './modules/project/services/project.service';
import { ProjectData, ProjectStorage } from './domain/project';
import { AuthenticationService } from './modules/authentication/services/authentication.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoading = true;
  isError: boolean;
  isNewProjectFormActive = false;
  hideSidebar = false;
  showLogoutButton = true;

  projects: ProjectData[] = [];

  constructor(
    public editMode: EditModeService,
    public router: Router,
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer,
    private renderer: Renderer2,
    private projectService: ProjectService,
    private storageService: StorageService,
    private authenticationService: AuthenticationService
  ) {
    this.matIconRegistry.addSvgIconSet(this.domSanitizer.bypassSecurityTrustResourceUrl('assets/icons/mdi-custom-icons.svg'));
    this.matIconRegistry.addSvgIconSet(this.domSanitizer.bypassSecurityTrustResourceUrl('assets/icons/tech-stack.svg'));
  }

  ngOnInit() {
    this.authenticationService.checkAndSetSsoMode();

    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (event.url === '/') {
          this.loadAllProjects();
        } else if (event.url === '/login' || event.url === '/logout') {
          this.showLogoutButton = false;
          this.isLoading = false;
        }
      }
    });
  }

  getEditModeStatus() {
    this.editMode.getEditModeFlag.subscribe((editMode: EditModeFlag) => {
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
    const projectKey = this.getProjectKeyFormStorage();
    if (projectKey) {
      this.router.navigateByUrl(`/project/${projectKey}`);
    } else {
      this.router.navigateByUrl('/project');
    }
  }

  private getProjectKeyFormStorage(): string | undefined {
    const projectKeyFromStorage = this.storageService.getItem('project') as ProjectStorage;
    return projectKeyFromStorage?.key;
  }

  private loadAllProjects() {
    this.projectService
      .getAllProjects()
      .pipe(
        catchError(() => {
          // show generic error page
          this.isLoading = false;
          this.isError = true;
          return EMPTY;
        })
      )
      .subscribe((response: ProjectData[]) => {
        this.projects = response;
        this.isLoading = false;
        this.isError = false;
        this.hideSidebar = false;
        this.checkRedirectToProjectDetail();
      });
  }
}
