import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { QuickstarterListComponent } from './quickstarter-list.component';
import { CommonModule } from '@angular/common';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
  selector: 'mat-icon',
  template: '<span></span>'
})
class MockMatIconComponent {
  @Input() svgIcon: any;
  @Input() fontSet: any;
  @Input() fontIcon: any;
}

describe('QuickstarterListComponent', () => {
  let component: QuickstarterListComponent;
  let fixture: ComponentFixture<QuickstarterListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatIconModule,
        MatTooltipModule,
        MatExpansionModule
      ],
      declarations: [QuickstarterListComponent]
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
  }));

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
    component.projectQuickstarters = [
      { description: 'qs-test1' },
      { description: 'qs-test2' }
    ] as any;
    /* when */
    fixture.detectChanges();
    /* then */
    const qsListElement = fixture.debugElement.query(
      By.css('[data-test-qs-list]')
    );
    const qsListEmptyElement = fixture.debugElement.query(
      By.css('[data-test-qs-list-empty]')
    );
    expect(qsListElement).toBeDefined();
    expect(qsListEmptyElement).toBeNull();
  });

  it('given no project quickstarters, should display message', () => {
    /* given */
    component.projectQuickstarters = null;
    /* when */
    fixture.detectChanges();
    /* then */
    const qsListElement = fixture.debugElement.query(
      By.css('[data-test-qs-list]')
    );
    const qsListEmptyElement = fixture.debugElement.query(
      By.css('[data-test-qs-list-empty]')
    );
    expect(qsListElement).toBeNull();
    expect(qsListEmptyElement).toBeDefined();
  });
});
