import { SidebarComponent } from './sidebar.component';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIconModule } from '@angular/material/icon';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../../app-form/app-form.module';
import { MatListModule } from '@angular/material/list';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { RouterTestingModule } from '@angular/router/testing';

// TODO fixme
describe('SidebarComponent', () => {
  const createComponent = createComponentFactory({
    component: SidebarComponent,
    imports: [
      CommonModule,
      RouterTestingModule,
      ReactiveFormsModule,
      FormsModule,
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
  });
  let component: any;
  let spectator: Spectator<SidebarComponent>;
  beforeEach(() => {
    spectator = createComponent({
      props: {
        isLoading: true
      }
    });
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should initially show all projects', () => {
    /* given */
    spectator.setInput({
      projects: {
        FOO1: {
          projectName: 'Foo Project 1',
          projectKey: 'FOO1'
        },
        FOO2: {
          projectName: 'Foo Project 2',
          projectKey: 'FOO2'
        },
        FOO3: {
          projectName: 'Foo Project 3',
          projectKey: 'FOO3'
        }
      } as any
    });
    /* when */
    /* then */
    console.log(spectator.query('.sidebar__header--count'));
    const itemHeader = spectator.query('.sidebar__header--count'); // null returned, but should be a HTML element
    expect(itemHeader.innerHTML).toContain('Projects (3)');
    // expect(spectator.query('[data-test-sidebar-nav-list-item]'))
  });

  it('should display only the PTE project when user enters "pte" search string', () => {
    /* given */
    /* when */
    /* then */
  });
});
