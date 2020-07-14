import { ProjectPageComponent } from './project-page.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { API_PROJECT_URL, API_PROJECT_DETAIL_URL } from '../../../tokens';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../../notification/notification.module';
import { BrowserService } from '../../browser/services/browser.service';

/* TODO fixme + add tests */
describe('ProjectPageComponent', () => {
  const createComponent = createComponentFactory({
    component: ProjectPageComponent,
    imports: [
      CommonModule,
      RouterTestingModule,
      HttpClientTestingModule,
      LoadingIndicatorModule,
      MatIconModule,
      MatFormFieldModule,
      MatInputModule,
      MatTooltipModule,
      MatButtonModule,
      MatTableModule,
      MatSortModule,
      ClipboardModule,
      NotificationModule
    ],
    providers: [
      { provide: API_PROJECT_DETAIL_URL, useValue: '/api/mock' },
      { provide: API_PROJECT_URL, useValue: '/api/mock' },
      {
        provide: BrowserService,
        useValue: {
          scrollIntoViewById: jest.fn()
        }
      }
    ]
  });
  let component: any;
  let spectator: Spectator<ProjectPageComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should show section to add new quickstarters when edit-button is clicked', () => {
    /* given */
    /* when */
    spectator.click('[data-test-edit-btn]');
    spectator.detectComponentChanges();
    /* then */
    expect(spectator.query('#new')).toBeVisible();
  });
});
