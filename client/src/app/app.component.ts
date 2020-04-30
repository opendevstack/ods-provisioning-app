import { Component, OnInit, Renderer2 } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { EditModeService } from './modules/edit-mode/services/edit-mode.service';
import { ProjectService } from './modules/project-page/services/project.service';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { Project } from './modules/project-page/domain/project';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  isLoading = true;
  isError: boolean;

  projects: Project[] = [];

  constructor(
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer,
    private renderer: Renderer2,
    private editModeService: EditModeService,
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
      .subscribe((response: Project[]) => {
        this.projects = response;
        this.isLoading = false;
        this.isError = false;
      });
  }

  getEditModeStatus() {
    this.editModeService.onGetEditModeFlag.subscribe(editModeActive => {
      if (editModeActive) {
        this.renderer.addClass(document.body, 'status-editmode-active');
      } else {
        this.renderer.removeClass(document.body, 'status-editmode-active');
      }
    });
  }
}
