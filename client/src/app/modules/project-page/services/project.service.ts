import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from '../tokens';
import { Project } from '../domain/project';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  constructor(
    private httpClient: HttpClient,
    @Inject(API_PROJECT_URL) private projectUrl: string,
    @Inject(API_ALL_PROJECTS_URL) private allProjectsUrl: string
  ) {}

  getAllProjects(): Observable<Project[]> {
    return this.httpClient.get(this.allProjectsUrl).pipe(
      map(projects => this.sortProjectsByName(this.json2array(projects))),
      tap(projects => {
        console.log('Projects: ', projects);
      }),
      catchError(this.handleError)
    );
  }

  getProjectByKey(projectKey: string): Observable<Project> {
    const projectUrl = this.replaceUrlPlaceholder(this.projectUrl, projectKey);
    return this.httpClient
      .get<Project>(projectUrl)
      .pipe(catchError(this.handleError));
  }

  private json2array(json: Object): Project[] {
    const result = [];
    if (json) {
      const keys = Object.keys(json);
      keys.forEach(function (key) {
        result.push(json[key]);
      });
    }
    return result;
  }

  private sortProjectsByName(projects: Project[]): Project[] {
    return projects.sort((a: Project, b: Project) => {
      return a.projectName.localeCompare(b.projectName);
    });
  }

  private replaceUrlPlaceholder(url: string, projectKey: string): string {
    return url.replace(/{{{PROJECT_KEY}}}/, `${projectKey}`);
  }

  private handleError(err: any) {
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
}
