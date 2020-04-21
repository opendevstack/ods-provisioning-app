import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { QuickstarterListComponent } from './quickstarter-list.component';

describe('QuickstarterListComponent', () => {
  let component: QuickstarterListComponent;
  let fixture: ComponentFixture<QuickstarterListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [QuickstarterListComponent]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QuickstarterListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
