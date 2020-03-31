import { ProjectPageComponent } from './project-page.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { API_PROJECT_URL } from '../tokens';
import { MatTooltipModule } from '@angular/material/tooltip';

// TODO fixme
xdescribe('ProjectPageComponent', () => {
  const createComponent = createComponentFactory({
    component: ProjectPageComponent,
    imports: [
      RouterTestingModule,
      HttpClientTestingModule,
      LoadingIndicatorModule,
      MatIconModule,
      MatFormFieldModule,
      MatInputModule,
      MatTooltipModule
    ],
    providers: [{ provide: API_PROJECT_URL, useValue: '/api/mock' }]
  });
  let component: any;
  let spectator: Spectator<ProjectPageComponent>;
  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
