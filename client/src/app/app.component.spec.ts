import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { MatListModule } from '@angular/material/list';
import { LoadingIndicatorModule } from './modules/loading-indicator/loading-indicator.module';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';

describe('AppComponent', () => {
  const createComponent = createComponentFactory({
    component: AppComponent,
    imports: [
      RouterTestingModule,
      MatListModule,
      MatCardModule,
      LoadingIndicatorModule
    ],
    providers: [MatDialog]
  });
  let component: any;
  let spectator: Spectator<AppComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});
