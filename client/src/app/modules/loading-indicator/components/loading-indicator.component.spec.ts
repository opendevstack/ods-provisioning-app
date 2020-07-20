import { LoadingIndicatorComponent } from './loading-indicator.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CommonModule } from '@angular/common';

describe('LoadingIndicatorComponent', () => {
  const createComponent = createComponentFactory({
    component: LoadingIndicatorComponent,
    imports: [CommonModule, MatProgressBarModule]
  });
  let component: any;
  let spectator: Spectator<LoadingIndicatorComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should add class "is-loading" to body on init', () => {
    spectator.component.ngOnInit();
    const body = spectator.query('body', { root: true });
    expect(body).toHaveClass('is-loading');
  });

  it('should remove class "is-loading" from body on destroy', () => {
    spectator.component.ngOnDestroy();
    const body = spectator.query('body', { root: true });
    expect(body).not.toHaveClass('is-loading');
  });
});
