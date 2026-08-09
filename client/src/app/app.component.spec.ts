import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { SidebarModule } from './modules/sidebar/sidebar.module';
import { MatDialog } from '@angular/material/dialog';
import { API_GENERATE_PROJECT_KEY_URL, API_PROJECT_DETAIL_URL, API_PROJECT_TEMPLATES_URL, API_PROJECT_URL } from './tokens';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { StorageService } from './modules/storage/services/storage.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  let mockStorageService;

  beforeEach(async () => {
    mockStorageService = jasmine.createSpyObj(['getItem', 'setItem']);
    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [HttpClientTestingModule, RouterTestingModule, SidebarModule, MatButtonModule, MatIconModule],
      providers: [
        MatDialog,
        { provide: API_PROJECT_DETAIL_URL, useValue: '/api/mock' },
        { provide: API_PROJECT_TEMPLATES_URL, useValue: '/api/mock' },
        { provide: API_GENERATE_PROJECT_KEY_URL, useValue: '/api/mock' },
        { provide: API_PROJECT_URL, useValue: '/api/mock' },
        { provide: StorageService, useValue: mockStorageService }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
