import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NewProjectComponent } from './new-project.component';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { ProjectModule } from '../../project/project.module';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { NotificationModule } from '../../notification/notification.module';
import { MatExpansionModule } from '@angular/material/expansion';
import { ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../../app-form/app-form.module';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import {
  API_GENERATE_PROJECT_KEY_URL,
  API_PROJECT_DETAIL_URL,
  API_PROJECT_TEMPLATES_URL,
  API_PROJECT_URL
} from '../../../tokens';
import { StorageService } from '../../storage/services/storage.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('NewProjectComponent', () => {
  let component: NewProjectComponent;
  let fixture: ComponentFixture<NewProjectComponent>;

  const mockValidationConfig = {
    project: {
      name: {
        regex: '',
        errorMessages: []
      },
      key: {
        regex: '',
        errorMessages: []
      }
    },
    quickstarters: {
      componentName: {
        regex: '',
        errorMessages: []
      }
    }
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule,
        ProjectModule,
        LoadingIndicatorModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatTooltipModule,
        MatButtonModule,
        NotificationModule,
        MatExpansionModule,
        ReactiveFormsModule,
        AppFormModule,
        MatCheckboxModule
      ],
      providers: [
        { provide: API_PROJECT_DETAIL_URL, useValue: '/api/mock' },
        { provide: API_PROJECT_URL, useValue: '/api/mock' },
        { provide: API_PROJECT_TEMPLATES_URL, useValue: '/api/mock' },
        { provide: API_GENERATE_PROJECT_KEY_URL, useValue: '/api/mock' },
        {
          provide: StorageService,
          useValue: {
            saveItem: jest.fn()
          }
        }
      ],
      declarations: [NewProjectComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NewProjectComponent);
    component = fixture.componentInstance;
    component.validationConfig = mockValidationConfig;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
