import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class HttpRequestInterceptor implements HttpInterceptor {
  constructor() {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const credentials = {
      withCredentials: true
    };

    let httpOptions;

    switch (req.method) {
      case 'PUT':
      case 'POST':
        httpOptions = {
          ...credentials,
          headers: new HttpHeaders().set('Content-Type', 'application/json; charset=UTF-8')
        };
        break;
      case 'GET':
        httpOptions = {
          ...credentials,
          headers: new HttpHeaders().set('Accept', 'application/json; charset=UTF-8')
        };
        break;
      default:
        httpOptions = credentials;
    }

    req = req.clone(httpOptions);

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          console.log('auth');
        }

        // TODO
        // - Send error to logging service (backend + browser console <- based on env setting
        // - Eventually show error details in a modal dialog in the browser (based on an env setting)
        console.error('HttpRequestInterceptor: HTTP Error', error);

        return throwError(error);
      })
    ) as Observable<HttpEvent<any>>;
  }
}
