import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication/services/authentication.service';

@Injectable()
export class HttpRequestInterceptor implements HttpInterceptor {
  constructor(private router: Router, private authenticationService: AuthenticationService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const credentials = {
      withCredentials: true
    };

    let httpOptions;

    switch (req.method) {
      case 'PUT':
      case 'POST':
        if (req.url != '/j_security_check') {
          httpOptions = {
            ...credentials,
            headers: new HttpHeaders().set('Content-Type', 'application/json; charset=UTF-8')
          };
        }
        break;
      case 'GET':
        httpOptions = {
          ...credentials,
          headers: req.headers.append('Accept', 'application/json; charset=UTF-8')
        };
        break;
      default:
        httpOptions = credentials;
    }

    req = req.clone(httpOptions);

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.router.navigateByUrl(this.authenticationService.sso ? '/' : '/login');
        }

        // TODO Improvement idea:
        // - Send error to backend logging service
        // - Eventually show error details in a modal dialog in the browser (based on an env setting)
        console.error('HttpRequestInterceptor: HTTP Error', error);

        return throwError(error);
      })
    ) as Observable<HttpEvent<any>>;
  }
}
