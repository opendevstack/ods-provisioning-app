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
  @Output() onOpenDialog = new EventEmitter<string>();

  constructor() {}

  ngOnInit(): void {}

  emitActivateEditMode() {
    this.onActivateEditMode.emit(true);
  }

  emitOpenDialog(text) {
    this.onOpenDialog.emit(text);
  }
}
