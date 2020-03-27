import { AboutPageComponent } from './about-page.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { MatListModule } from '@angular/material/list';

describe('AboutPageComponent', () => {
  const createComponent = createComponentFactory({
    component: AboutPageComponent,
    imports: [RouterTestingModule, MatListModule]
  });
  let component: any;
  let spectator: Spectator<AboutPageComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
