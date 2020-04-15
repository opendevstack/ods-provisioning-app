import { Component, Input } from '@angular/core';
import { Project } from '../../project-page/domain/project';
import { FormControl } from '@angular/forms';
import { map, startWith } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() projects: Project[];
  @Input() isError: boolean;
  @Input() isLoading: boolean;

  searchControl: FormControl = new FormControl();
  filteredProjects: Observable<Project[]>;

  constructor() {
    this.filteredProjects = this.searchControl.valueChanges.pipe(
      startWith(''),
      map(project =>
        project ? this.filterProjects(project) : this.projects.slice()
      )
    );
  }

  private filterProjects(value: string): Project[] {
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
