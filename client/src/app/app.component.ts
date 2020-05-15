import { Component, OnInit, Renderer2 } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { EditMode } from './modules/edit-mode/services/edit-mode.service';
import { ProjectService } from './modules/project-page/services/project.service';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { ProjectData } from './modules/project-page/domain/project';
import { EditModeFlag } from './modules/edit-mode/domain/edit-mode';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  isLoading = true;
  isError: boolean;
  isNewProjectFormActive = false;

  projects: ProjectData[] = [];

  constructor(
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer,
    private renderer: Renderer2,
    public editMode: EditMode,
    private projectService: ProjectService
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
    this.loadAllProjects();
  }

  loadAllProjects() {
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
}
