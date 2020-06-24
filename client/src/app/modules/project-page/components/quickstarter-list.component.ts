import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output
} from '@angular/core';
import { ProjectQuickstarter } from '../../../domain/quickstarter';
import { Subject } from 'rxjs';

@Component({
  selector: 'project-quickstarter-list',
  templateUrl: './quickstarter-list.component.html',
  styleUrls: ['./quickstarter-list.component.scss']
})
export class QuickstarterListComponent implements OnDestroy {
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Output() onActivateEditMode = new EventEmitter<boolean>();
  destroy$ = new Subject<boolean>();

  emitActivateEditMode() {
    this.onActivateEditMode.emit(true);
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
