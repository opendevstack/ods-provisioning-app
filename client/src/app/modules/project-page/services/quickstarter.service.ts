import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_ALL_QUICKSTARTERS_URL, API_PROJECT_URL } from '../../../tokens';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ProjectQuickstarter,
  ProjectQuickstarterComponent,
  ProjectQuickstarterComponentsData,
  QuickstarterData
} from '../../../domain/quickstarter';
import { DeleteComponentRequest } from '../../../domain/project';

@Injectable({
  providedIn: 'root'
})
export class QuickstarterService {
  private static groupProjectQuickstartersByDescription(quickstarters: ProjectQuickstarterComponentsData[]): ProjectQuickstarter[] {
    return quickstarters.reduce((acc, quickstarter: ProjectQuickstarterComponentsData) => {
      const quickstarterComponentObj: ProjectQuickstarterComponent = this.mapQuickstarterComponentsToFeModel(quickstarter);

      const found = acc.find(predicate => predicate.description === quickstarter.component_description);

      if (found) {
        found.ids.push(quickstarterComponentObj);
      } else {
        acc.push({
          description: quickstarter.component_description,
          type: quickstarter.component_type,
          ids: [quickstarterComponentObj]
        });
      }
      return acc;
    }, []);
  }

  private static sortByDescription = (a: { description: string }, b: { description: string }) => a.description.localeCompare(b.description);

  private static sortProjectQuickstartersByDescription(quickstarters: ProjectQuickstarter[]): ProjectQuickstarter[] {
    return quickstarters.sort(this.sortByDescription);
  }

  private static sortQuickstartersByDescription(quickstarters: QuickstarterData[]): QuickstarterData[] {
    return quickstarters.sort(this.sortByDescription);
  }

  // TODO consider lodash to shorten mapping
  private static mapQuickstarterComponentsToFeModel(quickstarter: ProjectQuickstarterComponentsData): ProjectQuickstarterComponent {
    return {
      id: quickstarter.component_id,
      groupId: quickstarter.GROUP_ID,
      odsGitRef: quickstarter.ODS_GIT_REF,
      odsImageTag: quickstarter.ODS_IMAGE_TAG,
      packageName: quickstarter.PACKAGE_NAME,
      projectId: quickstarter.PROJECT_ID,
      componentDescription: quickstarter.component_description,
      componentId: quickstarter.component_id,
      componentType: quickstarter.component_type,
      gitUrlHttp: quickstarter.git_url_http,
      gitUrlSsh: quickstarter.git_url_ssh
    };
  }

  constructor(
    private httpClient: HttpClient,
    @Inject(API_ALL_QUICKSTARTERS_URL) private apiAllQuickstartersUrl: string,
    @Inject(API_PROJECT_URL) private projectUrl: string
  ) {}

  getAllQuickstarters(): Observable<QuickstarterData[]> {
    return this.httpClient.get(this.apiAllQuickstartersUrl).pipe(
      map(json => (json ? Object.values(json) : []) as QuickstarterData[]),
      map(quickstarters => QuickstarterService.sortQuickstartersByDescription(quickstarters)),
      map(quickstarters => quickstarters.filter(quickstarter => quickstarter.enabled))
    );
  }

  transformProjectQuickstarterData(projectQuickstarters: ProjectQuickstarterComponentsData[]): ProjectQuickstarter[] {
    const groupedProjectQuickstarters = QuickstarterService.groupProjectQuickstartersByDescription(projectQuickstarters);
    return QuickstarterService.sortProjectQuickstartersByDescription(groupedProjectQuickstarters);
  }

  deleteQuickstarterComponent(componentDeleteObj: DeleteComponentRequest): Observable<DeleteComponentRequest> {
    return this.httpClient.put<DeleteComponentRequest>(this.projectUrl, componentDeleteObj);
  }
}
