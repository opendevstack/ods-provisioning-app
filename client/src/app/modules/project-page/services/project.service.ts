import {Inject, Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {catchError} from "rxjs/operators";
import {Observable, of, throwError} from "rxjs";
import {Project} from "../domain/project";
import {API_PROJECT_URL} from "../tokens";

@Injectable({
  providedIn: 'root'
})
export class ProjectService {

  constructor(private httpClient: HttpClient,
              @Inject(API_PROJECT_URL) private projectUrl: string) { }

  getProjectById(projectKey: string): Observable<Project[]> {
    const projectUrl = this.replaceUrlPlaceholder(this.projectUrl, projectKey);
    return this.httpClient.get<Project[]>(projectUrl).pipe(
      catchError(this.handleError<Project[]>('getProjectById'))
    );
  }

  private replaceUrlPlaceholder(url: string, projectKey: string): string {
    return url.replace(/{{{PROJECT_KEY}}}/, `${projectKey}`)
  }

  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {

      console.log(error);

      // TODO: send the error to remote logging infrastructure
      console.error(error);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }

}
