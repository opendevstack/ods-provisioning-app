import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';

import { Observable } from 'rxjs';

export enum HttpErrorTypes {
  UNKNOWN = 'UNKNOWN',
  NOT_FOUND = 'NOT_FOUND'
}

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

    return next.handle(req);
  }
}
