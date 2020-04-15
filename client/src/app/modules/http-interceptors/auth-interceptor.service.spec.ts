import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './auth-interceptor.service';
import { ProjectService } from '../project-page/services/project.service';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from '../project-page/tokens';
import { BrowserService } from '../browser/services/browser.service';
import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';

describe(`AuthHttpInterceptor`, () => {
  let spectator: SpectatorHttp<AuthInterceptor>;
  const createService = createHttpFactory({
    service: AuthInterceptor,
    mocks: [ProjectService],
    providers: [
      {
        provide: HTTP_INTERCEPTORS,
        useClass: AuthInterceptor,
        multi: true
      },
      { provide: API_PROJECT_URL, useValue: '/mock/url' },
      { provide: API_ALL_PROJECTS_URL, useValue: '/mock/url' },
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
