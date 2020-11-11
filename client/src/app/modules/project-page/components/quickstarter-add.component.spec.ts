import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { QuickstarterAddComponent } from './quickstarter-add.component';

describe('QuickstarterAddComponent', () => {
  let component: QuickstarterAddComponent;
  let fixture: ComponentFixture<QuickstarterAddComponent>;

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        declarations: [QuickstarterAddComponent]
      }).compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(QuickstarterAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
