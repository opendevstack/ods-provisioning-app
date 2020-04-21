import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpEventType,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';

import { Observable, throwError } from 'rxjs';
import { BrowserService } from '../browser/services/browser.service';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export enum HttpErrorTypes {
  UNKNOWN = 'Unknown',
  NOT_FOUND = 'Not found',
  UNAUTHORIZED = 'Unauthorized',
  NOT_ALLOWED = 'Not allowed'
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private browserService: BrowserService) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    if (
      req.url === environment.apiProjectUrl ||
      req.url === environment.apiAllProjectsUrl
    ) {
      console.log(`AuthInterceptor for: ${req.url}`);
      const authToken = this.browserService.getCookie('crowd.token_key');

      if (authToken && authToken.value) {
        const authRequest = req.clone({
          setHeaders: { Authorization: authToken.value }
        });

        return next.handle(authRequest).pipe(
          tap(event => {
            if (event.type === HttpEventType.Response) {
              console.log(event);
            }
          }),
          catchError((error: Observable<HttpEvent<any>>) => {
            if (error instanceof HttpErrorResponse) {
              if (error.status === 404) {
                return throwError({
                  message: HttpErrorTypes.NOT_FOUND
                });
              }

              if (error.status === 401) {
                return throwError({
                  message: HttpErrorTypes.UNAUTHORIZED
                });
              }

              if (error.status === 405) {
                return throwError({
                  message: HttpErrorTypes.NOT_ALLOWED
                });
              }

              return throwError({
                message: HttpErrorTypes.UNKNOWN
              });
            }
          })
        );
      }
    }

    return next.handle(req);
  }
}
