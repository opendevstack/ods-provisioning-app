import {Injectable} from '@angular/core';
import {
  HttpEvent,
  HttpInterceptor,
  HttpHandler,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';

import {Observable, of, throwError} from 'rxjs';
import {BrowserService} from "../browser/services/browser.service";
import {catchError} from "rxjs/operators";

export enum HttpErrorTypes {
  UNKNOWN = 'unknown',
  NOT_FOUND = 'not found',
  UNAUTHORIZED = 'unauthorized'
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private browserService: BrowserService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    const authToken = this.browserService.getCookie('crowd.token_key').value;

    if (authToken) {
      const authRequest = req.clone({
        setHeaders: {Authorization: authToken}
      });

      return next.handle(authRequest).pipe(
        catchError((error: Observable<HttpEvent<any>>) => {
        if (error instanceof HttpErrorResponse) {
          if (error.status === 0) {
            return throwError({
              message: HttpErrorTypes.UNKNOWN
            });
          }

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
        }
      }));

    }

  }

}
