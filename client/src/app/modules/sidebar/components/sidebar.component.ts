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
    const filterValue = new RegExp(value.toLowerCase().trim());

    return this.projects.filter(
      project =>
        filterValue.test(project.projectKey.toLowerCase()) ||
        filterValue.test(project.projectName.toLowerCase()) ||
        filterValue.test(project.description.toLowerCase())
    );
  }

  canDisplayContent(): boolean {
    return !this.isLoading && !this.isError;
  }
}
