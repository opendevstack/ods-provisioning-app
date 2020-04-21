import { NotificationComponent } from './notification.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';

// TODO fixme + add tests
xdescribe('NotificationComponent', () => {
  const createComponent = createComponentFactory({
    component: NotificationComponent,
    imports: [CommonModule, MatDialogModule]
  });
  let component: any;
  let spectator: Spectator<NotificationComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
