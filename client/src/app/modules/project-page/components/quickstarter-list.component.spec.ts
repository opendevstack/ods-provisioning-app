import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { QuickstarterListComponent } from './quickstarter-list.component';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { By } from '@angular/platform-browser';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { API_ALL_QUICKSTARTERS_URL, API_PROJECT_URL } from '../../../tokens';
import { MatIconTestingModule } from '@angular/material/icon/testing';

describe('QuickstarterListComponent', () => {
  let component: QuickstarterListComponent;
  let fixture: ComponentFixture<QuickstarterListComponent>;

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [CommonModule, HttpClientTestingModule, MatIconTestingModule, MatTooltipModule, MatExpansionModule, MatDialogModule],
        declarations: [QuickstarterListComponent, MatIcon],
        providers: [
          { provide: API_ALL_QUICKSTARTERS_URL, useValue: '/api/mock' },
          { provide: API_PROJECT_URL, useValue: '/api/mock' }
        ]
      }).compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(QuickstarterListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('given project quickstarters, should display quickstarters', () => {
    /* given */
    component.projectQuickstarters = [{ description: 'qs-test1' }, { description: 'qs-test2' }] as any;
    /* when */
    fixture.detectChanges();
    /* then */
    const qsListElement = fixture.debugElement.query(By.css('[data-test-qs-list]'));
    const qsListEmptyElement = fixture.debugElement.query(By.css('[data-test-qs-list-empty]'));
    expect(qsListElement).toBeDefined();
    expect(qsListEmptyElement).toBeNull();
  });

  describe('given no project quickstarters', () => {
    beforeEach(() => {
      component.projectQuickstarters = null;
    });

    it('when platform runtime is existing, should display message + button to add quickstarters', () => {
      /* given */
      component.hasPlatformRuntime = true;
      /* when */
      fixture.detectChanges();
      /* then */
      const qsListWithPlatformRuntimeElement = fixture.debugElement.query(By.css('[data-test-qs-list-platform-runtime-yes]'));
      const qsListWithoutPlatformRuntimeElement = fixture.debugElement.query(By.css('[data-test-qs-list-platform-runtime-no]'));
      const addQsButtonElement = fixture.debugElement.query(By.css('[data-test-add-qs-btn]'));
      expect(qsListWithPlatformRuntimeElement).toBeDefined();
      expect(qsListWithoutPlatformRuntimeElement).toBeNull();
      expect(addQsButtonElement).toBeDefined();
    });

    it('when platform runtime is not existing, should display message without add button', () => {
      /* given */
      component.hasPlatformRuntime = false;
      /* when */
      fixture.detectChanges();
      /* then */
      const qsListWithPlatformRuntimeElement = fixture.debugElement.query(By.css('[data-test-qs-list-platform-runtime-yes]'));
      const qsListWithoutPlatformRuntimeElement = fixture.debugElement.query(By.css('[data-test-qs-list-platform-runtime-no]'));
      const addQsButtonElement = fixture.debugElement.query(By.css('[data-test-add-qs-btn]'));
      expect(qsListWithPlatformRuntimeElement).toBeNull();
      expect(qsListWithoutPlatformRuntimeElement).toBeDefined();
      expect(addQsButtonElement).toBeNull();
    });
  });
});
