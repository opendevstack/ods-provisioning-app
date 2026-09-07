import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectPageComponent } from './project-page.component';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../../notification/notification.module';
import {
  API_ALL_QUICKSTARTERS_URL,
  API_GENERATE_PROJECT_KEY_URL,
  API_PROJECT_DETAIL_URL,
  API_PROJECT_TEMPLATES_URL,
  API_PROJECT_URL
} from '../../../tokens';
import { BrowserService } from '../../browser/services/browser.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { StorageService } from '../../storage/services/storage.service';
import { MatIconTestingModule } from '@angular/material/icon/testing';

describe('ProjectPageComponent', () => {
  let component: ProjectPageComponent;
  let fixture: ComponentFixture<ProjectPageComponent>;

  let mockBrowserService;
  let mockStorageService;

  beforeEach(async () => {
    mockStorageService = jasmine.createSpyObj('StorageService', ['getItem']);

    await TestBed.configureTestingModule({
      declarations: [ProjectPageComponent],
      imports: [
        ReactiveFormsModule,
        FormsModule,
        RouterTestingModule,
        HttpClientTestingModule,
        LoadingIndicatorModule,
        MatIconTestingModule,
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
        { provide: API_PROJECT_TEMPLATES_URL, useValue: '/api/mock' },
        { provide: API_GENERATE_PROJECT_KEY_URL, useValue: '/api/mock' },
        { provide: API_ALL_QUICKSTARTERS_URL, useValue: '/api/mock' },
        { provide: API_PROJECT_URL, useValue: '/api/mock' },
        {
          provide: BrowserService,
          useValue: mockBrowserService
        },
        {
          provide: StorageService,
          useValue: mockStorageService
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should show section to add new quickstarters when edit-button is clicked', () => {
    /* given */
    /* when */
    // spectator.click('[data-test-edit-btn]');
    // spectator.detectComponentChanges();
    /* then */
    // expect(spectator.query('#new')).toBeVisible();
  });
});
