import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadingIndicatorComponent } from './loading-indicator.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';

describe('LoadingIndicatorComponent', () => {
  let component: LoadingIndicatorComponent;
  let fixture: ComponentFixture<LoadingIndicatorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [MatProgressBarModule],
      declarations: [LoadingIndicatorComponent]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoadingIndicatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should add class "is-loading" to body on init', () => {
    expect(document.body.classList).toContain('is-loading');
  });

  it('should remove class "is-loading" from body on destroy', () => {
    component.ngOnDestroy();
    expect(document.body.classList).not.toContain('is-loading');
  });
});
