import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ProjectData, ProjectLink } from '../../../domain/project';

@Component({
  selector: 'project-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class ProjectHeaderComponent implements OnInit {
  @Input() project: ProjectData;
  @Input() projectLinks: ProjectLink[];
  @Input() editMode: boolean;
  @Input() isQuickstartersError: boolean;
  @Input() aggregatedProjectLinks: string;

  @Output() onActivateEditMode = new EventEmitter<boolean>();
  @Output() onOpenNotification = new EventEmitter<string>();

  constructor() {}

  ngOnInit(): void {}

  emitActivateEditMode(flag: boolean) {
    this.onActivateEditMode.emit(flag);
  }

  emitOpenNotification(text) {
    this.onOpenNotification.emit(text);
  }

  isEditingPossible(): boolean {
    return this.project.platformRuntime && !this.isQuickstartersError;
  }
}
