import { ErrorHandler, Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor() {}

  handleError(error: Error) {
    if (!(error instanceof HttpErrorResponse)) {
      // TODO Handle Client Error (Angular Error, ReferenceError...):
      // - Send error to logging service (backend + browser console <- based on env setting
      // - Navigate to separate error page
      console.error('GlobalErrorHandler: CLIENT Error', error);
    } else {
      // Nothing to do here as the HttpRequestInterceptor takes care of HTTP errors
    }

    // TODO Eventually show error details in a modal dialog in the browser (based on an env setting)
  }
}
