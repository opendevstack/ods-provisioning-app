import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { MatListModule } from '@angular/material/list';
import { LoadingIndicatorModule } from './modules/loading-indicator/loading-indicator.module';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { SidebarModule } from './modules/sidebar/sidebar.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from './tokens';

describe('AppComponent', () => {
  const createComponent = createComponentFactory({
    component: AppComponent,
    imports: [
      HttpClientTestingModule,
      RouterTestingModule,
      MatListModule,
      MatCardModule,
      LoadingIndicatorModule,
      SidebarModule
    ],
    providers: [
      MatDialog,
      { provide: API_PROJECT_URL, useValue: '/api/mock' },
      { provide: API_ALL_PROJECTS_URL, useValue: '/api/mock' }
    ]
  });
  let component: any;
  let spectator: Spectator<AppComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});
