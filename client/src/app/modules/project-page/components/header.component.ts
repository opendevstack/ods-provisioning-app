import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ProjectData, ProjectLink } from '../../../domain/project';

@Component({
  selector: 'app-project-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class ProjectHeaderComponent {
  @Input() project: ProjectData;
  @Input() projectLinks: ProjectLink[];
  @Input() editMode: boolean;
  @Input() isQuickstartersError: boolean;
  @Input() aggregatedProjectLinks: string;

  @Output() activateEditMode = new EventEmitter<boolean>();
  @Output() openNotification = new EventEmitter<string>();

  constructor() {}

  emitActivateEditMode(flag: boolean) {
    this.activateEditMode.emit(flag);
  }

  emitOpenNotification(text) {
    this.openNotification.emit(text);
  }

  isEditingPossible(): boolean {
    return this.project.platformRuntime && !this.isQuickstartersError;
  }
}
