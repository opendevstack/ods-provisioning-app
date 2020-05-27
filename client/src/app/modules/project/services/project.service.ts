import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import {
  API_PROJECT_URL,
  API_GENERATE_PROJECT_KEY_URL,
  API_PROJECT_DETAIL_URL,
  API_PROJECT_TEMPLATES_URL
} from '../../../tokens';
import {
  UpdateProjectRequest,
  ProjectData,
  ProjectLink,
  ProjectTemplate,
  ProjectKeyResponse,
  NewProjectRequest
} from '../../../domain/project';
import { default as projectLinksConfig } from '../config/project-links.conf.json';
import { HttpErrorTypes } from '../../http-interceptors/http-request-interceptor.service';

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

  private static handleError(err: HttpErrorResponse): Observable<any> {
    // TODO: send the error to remote logging infrastructure
    switch (err.status) {
      case 404:
        return throwError(HttpErrorTypes.NOT_FOUND);
      default:
        return throwError(HttpErrorTypes.UNKNOWN);
    }
  }

  constructor(
    private httpClient: HttpClient,
    @Inject(API_PROJECT_DETAIL_URL) private projectDetailUrl: string,
    @Inject(API_PROJECT_URL) private projectUrl: string,
    @Inject(API_PROJECT_TEMPLATES_URL) private projectTemplatesUrl: string,
    @Inject(API_GENERATE_PROJECT_KEY_URL) private generateProjectKeyUrl: string
  ) {}

  getAllProjects(): Observable<ProjectData[]> {
    return this.httpClient.get(this.projectUrl).pipe(
      map(json => (json ? Object.values(json) : []) as ProjectData[]),
      map(projects => ProjectService.sortProjectsByName(projects)),
      // tap(projects => console.log('Projects: ', projects)),
      catchError(ProjectService.handleError)
    );
  }

  getProjectByKey(projectKey: string): Observable<ProjectData> {
    const projectUrl = ProjectService.replaceUrlPlaceholder(
      this.projectDetailUrl,
      projectKey
    );
    return this.httpClient
      .get<ProjectData>(projectUrl)
      .pipe(catchError(ProjectService.handleError));
  }

  updateProject(project: UpdateProjectRequest): Observable<ProjectData> {
    return this.httpClient
      .put<UpdateProjectRequest>(this.projectUrl, project)
      .pipe(catchError(ProjectService.handleError));
  }

  createProject(project: NewProjectRequest): Observable<ProjectData> {
    return this.httpClient
      .post<NewProjectRequest>(this.projectUrl, project)
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

  getAggregateProjectLinks(projectLinks: ProjectLink[]): string | undefined {
    return projectLinks
      ?.filter(link => link.url)
      .map(link => link.url)
      .join('\n');
  }

  getProjectTemplates(): Observable<ProjectTemplate[]> {
    return this.httpClient
      .get<ProjectTemplate[]>(this.projectTemplatesUrl)
      .pipe(
        map(result => result['project-template-keys']),
        catchError(ProjectService.handleError)
      );
  }

  generateProjectKey(projectName: string): Observable<ProjectKeyResponse> {
    return this.httpClient
      .get<ProjectKeyResponse>(this.generateProjectKeyUrl, {
        params: { name: projectName }
      })
      .pipe(catchError(ProjectService.handleError));
  }
}
