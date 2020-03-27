import { async, inject, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './auth-interceptor.service';
import { ProjectService } from '../project-page/services/project.service';
import { API_PROJECT_URL } from '../project-page/tokens';
import { BrowserService } from '../browser/services/browser.service';

// TODO fixme
xdescribe(`AuthHttpInterceptor`, () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ProjectService,
        {
          provide: HTTP_INTERCEPTORS,
          useClass: AuthInterceptor,
          multi: true
        },
        { provide: API_PROJECT_URL, useValue: '/mock/url' },
        {
          provide: BrowserService,
          useValue: { getCookie: jest.fn() }
        }
      ]
    });
  }));

  it('should add an Authorization header', inject(
    [HttpTestingController, ProjectService],
    (httpMock: HttpTestingController, projectService: ProjectService) => {
      projectService.getProjectById('ASAP').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const httpRequest = httpMock.expectOne({
        url: '/mock/url/ASAP',
        method: 'GET'
      });
      httpMock.verify();

      expect(httpRequest.request.headers.has('Authorization')).toEqual(true);
    }
  ));
});
