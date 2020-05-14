import { Component, Input, OnInit } from '@angular/core';
import { ProjectData } from '../../project-page/domain/project';
import { FormControl } from '@angular/forms';
import { map, startWith } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { ProjectService } from '../../project-page/services/project.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() projects: ProjectData[];
  @Input() isError: boolean;
  @Input() isLoading: boolean;
  @Input() isNewProjectFormActive: boolean;

  searchControl: FormControl = new FormControl();
  filteredProjects: Observable<ProjectData[]>;

  constructor(private projectService: ProjectService, public router: Router) {
    this.filteredProjects = this.searchControl.valueChanges.pipe(
      startWith(''),
      map(project =>
        project ? this.filterProjects(project) : this.projects.slice()
      )
    );
  }

  private filterProjects(value: string): ProjectData[] {
    return this.projects.filter(
      project =>
        project.projectKey.toLowerCase().indexOf(value.toLowerCase().trim()) >=
          0 ||
        project.projectName.toLowerCase().indexOf(value.toLowerCase().trim()) >=
          0 ||
        project.description.toLowerCase().indexOf(value.toLowerCase().trim()) >=
          0
    );
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isError;
  }
}
