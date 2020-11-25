import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { API_PROJECT_URL, API_GENERATE_PROJECT_KEY_URL, API_PROJECT_DETAIL_URL, API_PROJECT_TEMPLATES_URL } from '../../../tokens';
import {
  UpdateProjectRequest,
  ProjectData,
  ProjectLink,
  ProjectTemplate,
  ProjectKeyResponse,
  NewProjectRequest
} from '../../../domain/project';
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

  private static replaceUrlPlaceholder(url: string, projectKey: string): string {
    return url.replace(/{{{PROJECT_KEY}}}/, `${projectKey}`);
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
      map(projects => ProjectService.sortProjectsByName(projects))
    );
  }

  getProjectByKey(projectKey: string): Observable<ProjectData> {
    const projectUrl = ProjectService.replaceUrlPlaceholder(this.projectDetailUrl, projectKey);
    return this.httpClient.get<ProjectData>(projectUrl);
  }

  updateProject(project: UpdateProjectRequest): Observable<UpdateProjectRequest> {
    return this.httpClient.put<UpdateProjectRequest>(this.projectUrl, project);
  }

  createProject(project: NewProjectRequest): Observable<NewProjectRequest> {
    return this.httpClient.post<NewProjectRequest>(this.projectUrl, project);
  }

  deleteProject(projectKey: string): Observable<NewProjectRequest> {
    return this.httpClient.delete<NewProjectRequest>(this.projectUrl + '/' + projectKey);
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
    return this.httpClient.get<ProjectTemplate[]>(this.projectTemplatesUrl).pipe(map(result => result['project-template-keys']));
  }

  generateProjectKey(projectName: string): Observable<ProjectKeyResponse> {
    return this.httpClient.get<ProjectKeyResponse>(this.generateProjectKeyUrl, {
      params: { name: projectName }
    });
  }
}
