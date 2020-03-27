import { ProvisionPageComponent } from './provision-page.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { MatListModule } from '@angular/material/list';

describe('ProvisionPageComponent', () => {
  const createComponent = createComponentFactory({
    component: ProvisionPageComponent,
    imports: [RouterTestingModule, MatListModule]
  });
  let component: any;
  let spectator: Spectator<ProvisionPageComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
