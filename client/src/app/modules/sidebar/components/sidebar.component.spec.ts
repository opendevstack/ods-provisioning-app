import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SidebarComponent } from './sidebar.component';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatListModule } from '@angular/material/list';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { Component, Input } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../../app-form/app-form.module';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

@Component({
  selector: 'app-mat-icon',
  template: '<span></span>'
})
class MockMatIconComponent {
  @Input() svgIcon: any;
  @Input() fontSet: any;
  @Input() fontIcon: any;
}

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;

  function setupTestProjects() {
    component.projects = [
      {
        projectName: 'FooProject1',
        projectKey: 'FOO1'
      },
      {
        projectName: 'FooProject2',
        projectKey: 'FOO2'
      }
    ] as any;

    fixture.detectChanges();
  }

  function doSearchInput(searchText: string) {
    const searchInputElement = fixture.debugElement.query(By.css('[data-test-search-input]')).nativeElement;
    searchInputElement.value = searchText;
    searchInputElement.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function getSearchResultCountElement() {
    return fixture.debugElement.nativeElement.querySelector('[data-test-sidebar-project-count]');
  }

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        declarations: [SidebarComponent],
        imports: [
          NoopAnimationsModule,
          CommonModule,
          RouterTestingModule,
          ReactiveFormsModule,
          AppFormModule,
          MatCardModule,
          LoadingIndicatorModule,
          MatListModule,
          MatFormFieldModule,
          MatInputModule,
          MatAutocompleteModule,
          MatButtonModule,
          MatIconModule
        ]
      })
        .overrideModule(MatIconModule, {
          remove: {
            declarations: [MatIcon],
            exports: [MatIcon]
          },
          add: {
            declarations: [MockMatIconComponent],
            exports: [MockMatIconComponent]
          }
        })
        .compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display a message when projects cannot be loaded', () => {
    /* given */
    component.projects = null;
    /* when */

    /* then */
    const errorMessageElement = fixture.debugElement.nativeElement.querySelector('[data-test-error]');
    const sidebarElement = fixture.debugElement.nativeElement.querySelector('[data-test-sidebar]');
    expect(sidebarElement).toBeNull();
    expect(errorMessageElement).toBeDefined();
  });

  it('should be shown when projects could be loaded', () => {
    /* given */
    component.projects = null;
    /* when */

    /* then */
    const errorMessageElement = fixture.debugElement.nativeElement.querySelector('[data-test-error]');
    const sidebarElement = fixture.debugElement.nativeElement.querySelector('[data-test-sidebar]');
    expect(sidebarElement).toBeDefined();
    expect(errorMessageElement).toBeNull();
  });

  it('when entering search criteria, counter should display a remaining count in the header', () => {
    setupTestProjects();
    expect(getSearchResultCountElement().innerHTML).toContain('(2)');
    doSearchInput('ject2');
    expect(getSearchResultCountElement().innerHTML).toContain('(1)');
    doSearchInput('ject');
    expect(getSearchResultCountElement().innerHTML).toContain('(2)');
    doSearchInput('something');
    expect(getSearchResultCountElement().innerHTML).toContain('(0)');
  });
});
