import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHeaderComponent } from './header.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../../notification/notification.module';

describe('ProjectHeaderComponent', () => {
  const createComponent = createComponentFactory({
    component: ProjectHeaderComponent,
    imports: [
      CommonModule,
      RouterTestingModule,
      HttpClientTestingModule,
      LoadingIndicatorModule,
      MatIconModule,
      MatFormFieldModule,
      MatInputModule,
      MatCardModule,
      MatTooltipModule,
      MatButtonModule,
      MatTableModule,
      MatSortModule,
      ClipboardModule,
      NotificationModule
    ]
  });
  let component: any;
  let spectator: Spectator<ProjectHeaderComponent>;
  beforeEach(() => {
    spectator = createComponent({
      props: {
        project: {}
      }
    });
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
