import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpRequestInterceptor } from './http-request-interceptor.service';
import { ProjectService } from '../project/services/project.service';
import { API_PROJECT_URL, API_PROJECT_DETAIL_URL } from '../../tokens';
import { BrowserService } from '../browser/services/browser.service';
import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';

describe(`AuthHttpInterceptor`, () => {
  let spectator: SpectatorHttp<HttpRequestInterceptor>;
  const createService = createHttpFactory({
    service: HttpRequestInterceptor,
    mocks: [ProjectService],
    providers: [
      {
        provide: HTTP_INTERCEPTORS,
        useClass: HttpRequestInterceptor,
        multi: true
      },
      { provide: API_PROJECT_DETAIL_URL, useValue: '/mock/url' },
      { provide: API_PROJECT_URL, useValue: '/mock/url' },
      {
        provide: BrowserService,
        useValue: { getCookie: jest.fn() }
      }
    ]
  });

  beforeEach(() => (spectator = createService()));

  it('should be created', () => {
    expect(spectator.service).toBeTruthy();
  });
});
