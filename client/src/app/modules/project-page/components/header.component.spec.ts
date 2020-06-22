import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHeaderComponent } from './header.component';
import { ProjectModule } from '../../project/project.module';
import { LoadingIndicatorModule } from '../../loading-indicator/loading-indicator.module';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NotificationModule } from '../../notification/notification.module';
import { MatExpansionModule } from '@angular/material/expansion';
import { ReactiveFormsModule } from '@angular/forms';
import { AppFormModule } from '../../app-form/app-form.module';
import { Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
  selector: 'mat-icon',
  template: '<span></span>'
})
class MockMatIconComponent {
  @Input() svgIcon: any;
  @Input() fontSet: any;
  @Input() fontIcon: any;
}

describe('ProjectHeaderComponent', () => {
  let component: ProjectHeaderComponent;
  let fixture: ComponentFixture<ProjectHeaderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        ProjectModule,
        LoadingIndicatorModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatCardModule,
        MatTooltipModule,
        MatButtonModule,
        ClipboardModule,
        NotificationModule,
        MatExpansionModule,
        ReactiveFormsModule,
        AppFormModule
      ],
      declarations: [ProjectHeaderComponent]
    })
      .overrideModule(MatIconModule, {
        remove: {
          declarations: [MatIcon],
          exports: [MatIcon]
        },
        add: {
          declarations: [MockMatIconComponent],
          exports: [MockMatIconComponent]
        }
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHeaderComponent);
    component = fixture.componentInstance;
    component.project = {
      description: 'foo',
      projectName: 'Foo',
      projectKey: 'FOO',
      bugtrackerSpace: false,
      collaborationSpaceUrl: null,
      platformRuntime: false,
      scmvcsUrl: null,
      repositories: null,
      platformBuildEngineUrl: null,
      specialPermissionSet: null,
      lastExecutionJobs: null,
      physicalLocation: null
    };
    jest.spyOn(component.onActivateEditMode, 'emit');
    jest.spyOn(component.onOpenDialog, 'emit');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with editmode not active', () => {
    it('should display an "edit project" button', () => {
      /* given */
      component.editMode = false;
      /* when */
      fixture.detectChanges();
      /* then */
      const startEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-start-edit-project-btn]'
      );
      const closeEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-close-edit-project-btn]'
      );
      expect(startEditProjectBtn).toBeDefined();
      expect(closeEditProjectBtn).toBeNull();
    });

    it('should activate editmode on click', () => {
      /* given */
      component.editMode = false;
      /* when */
      fixture.detectChanges();
      const startEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-start-edit-project-btn]'
      );
      const closeEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-close-edit-project-btn]'
      );
      startEditProjectBtn.click();
      /* then */
      expect(closeEditProjectBtn).toBeDefined();
      expect(component.onActivateEditMode.emit).toHaveBeenCalledWith(true);
    });
  });

  describe('with editmode active', () => {
    it('should display an "close project" button', () => {
      /* given */
      component.editMode = true;
      /* when */
      fixture.detectChanges();
      /* then */
      const startEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-start-edit-project-btn]'
      );
      const closeEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-close-edit-project-btn]'
      );
      expect(startEditProjectBtn).toBeNull();
      expect(closeEditProjectBtn).toBeDefined();
    });

    it('should deactivate editmode on click', () => {
      /* given */
      component.editMode = true;
      /* when */
      fixture.detectChanges();
      const startEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-start-edit-project-btn]'
      );
      const closeEditProjectBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-close-edit-project-btn]'
      );
      closeEditProjectBtn.click();
      /* then */
      expect(startEditProjectBtn).toBeDefined();
      expect(component.onActivateEditMode.emit).toHaveBeenCalledWith(false);
    });
  });

  describe('having project links', () => {
    beforeEach(() => {
      component.aggregatedProjectLinks = 'foo';
      fixture.detectChanges();
    });

    it('should show copy to clipboard button', () => {
      /* then */
      const copyToClipboardBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-copy-to-clipboard-btn]'
      );
      expect(copyToClipboardBtn).toBeDefined();
    });

    it('should open copy to clipboard confirmation dialog when clicking on copy button', () => {
      /* when */
      const copyToClipboardBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-copy-to-clipboard-btn]'
      );
      copyToClipboardBtn.click();
      /* then */
      expect(component.onOpenDialog.emit).toHaveBeenCalled();
    });

    it('with applied urls, should show link to click on', () => {
      /* given */
      component.projectLinks = [
        {
          url: 'http://some.host',
          iconName: 'foo',
          iconLabel: 'foo'
        },
        {
          url: null,
          iconName: 'foo',
          iconLabel: 'foo'
        },
        {
          url: null,
          iconName: 'foo',
          iconLabel: 'foo'
        }
      ];
      /* when */
      fixture.detectChanges();
      /* then */
      const projectLinkUrl = fixture.debugElement.queryAll(
        By.css('[data-test-project-link-url]')
      );
      expect(projectLinkUrl.length).toBe(1);
    });

    it('without applied urls, should not show link to click on', () => {
      /* given */
      component.projectLinks = [
        {
          url: null,
          iconName: 'foo',
          iconLabel: 'foo'
        },
        {
          url: null,
          iconName: 'foo',
          iconLabel: 'foo'
        },
        {
          url: null,
          iconName: 'foo',
          iconLabel: 'foo'
        }
      ];
      /* when */
      fixture.detectChanges();
      /* then */
      const projectLinkUrl = fixture.debugElement.queryAll(
        By.css('[data-test-project-link-url]')
      );
      expect(projectLinkUrl.length).toBe(0);
    });
  });

  describe('not having project links', () => {
    beforeEach(() => {
      component.aggregatedProjectLinks = null;
      fixture.detectChanges();
    });

    it('should not show copy to clipboard button', () => {
      /* then */
      const copyToClipboardBtn = fixture.debugElement.nativeElement.querySelector(
        '[data-test-copy-to-clipboard-btn]'
      );
      expect(copyToClipboardBtn).toBeNull();
    });
  });
});
