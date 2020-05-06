import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from '../tokens';
import { ProjectData, ProjectLink } from '../domain/project';
import { default as projectLinksConfig } from '../config/project-links.conf.json';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private static sortProjectsByName(projects: ProjectData[]): ProjectData[] {
    return projects.sort((a: ProjectData, b: ProjectData) => {
      return a.projectName.localeCompare(b.projectName);
    });
  }

  private static replaceUrlPlaceholder(
    url: string,
    projectKey: string
  ): string {
    return url.replace(/{{{PROJECT_KEY}}}/, `${projectKey}`);
  }

  private static handleError(err: any) {
    let errorMsg: string;
    if (err.error instanceof ErrorEvent) {
      // Client error
      errorMsg = `An error occured: ${err.error}`;
    } else {
      // Backend error
      errorMsg = `Backend returned ${err.status}`;
    }
    // TODO: send the error to remote logging infrastructure
    return throwError(errorMsg);
  }

  constructor(
    private httpClient: HttpClient,
    @Inject(API_PROJECT_URL) private projectUrl: string,
    @Inject(API_ALL_PROJECTS_URL) private allProjectsUrl: string
  ) {}

  getAllProjects(): Observable<ProjectData[]> {
    return this.httpClient.get(this.allProjectsUrl).pipe(
      map(json => (json ? Object.values(json) : []) as ProjectData[]),
      map(projects => ProjectService.sortProjectsByName(projects)),
      // tap(projects => console.log('Projects: ', projects)),
      catchError(ProjectService.handleError)
    );
  }

  getProjectByKey(projectKey: string): Observable<ProjectData> {
    const projectUrl = ProjectService.replaceUrlPlaceholder(
      this.projectUrl,
      projectKey
    );
    return this.httpClient
      .get<ProjectData>(projectUrl)
      .pipe(catchError(ProjectService.handleError));
  }

  getProjectLinksConfig(project: ProjectData): ProjectLink[] {
    return projectLinksConfig.map(link => {
      return {
        url: project[link.urlKey],
        iconName: link.iconName,
        iconLabel: link.iconLabel
      };
    });
  }

  getAggregateProjectLinks(projectLinks: ProjectLink[]): string | null {
    return projectLinks?.map(link => link.url).join('\n');
  }
}
