import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output
} from '@angular/core';
import {
  ProjectQuickstarter,
  ProjectQuickStarterComponentDeleteObj
} from '../../../domain/quickstarter';
import { EMPTY, Subject } from 'rxjs';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ConfirmationComponent } from '../../confirmation/components/confirmation.component';
import { QuickstarterService } from '../services/quickstarter.service';
import { NotificationComponent } from '../../notification/components/notification.component';
import { DeleteComponentRequest } from '../../../domain/project';
import { ConfirmationConfig } from '../../confirmation/domain/confirmation-config';

@Component({
  selector: 'project-quickstarter-list',
  templateUrl: './quickstarter-list.component.html',
  styleUrls: ['./quickstarter-list.component.scss']
})
export class QuickstarterListComponent implements OnDestroy {
  @Input() projectQuickstarters: ProjectQuickstarter[];
  @Input() projectKey: string;
  @Output() onActivateEditMode = new EventEmitter<boolean>();
  @Output() triggerLoadProjectData = new EventEmitter<boolean>();
  destroy$ = new Subject<boolean>();

  private static buildConfirmationConfig(
    componentId: string
  ): ConfirmationConfig {
    return {
      verify: {
        inputLabel: 'Component name',
        compareValue: componentId
      },
      text: {
        title: 'Remove component',
        info:
          "This will delete the component including its Jira spaces and component's cd/dev/test namespaces in OpenShift",
        ctaButtonLabel: 'Yes, remove component'
      }
    };
  }

  constructor(
    private dialog: MatDialog,
    private quickstarterService: QuickstarterService
  ) {}

  emitActivateEditMode() {
    this.onActivateEditMode.emit(true);
  }

  intendDeleteComponent(
    componentDeleteObj: ProjectQuickStarterComponentDeleteObj
  ) {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = QuickstarterListComponent.buildConfirmationConfig(
      componentDeleteObj.id
    );
    dialogConfig.panelClass = 'custom-dialog-panel';
    const dialogRef = this.dialog.open(ConfirmationComponent, dialogConfig);
    dialogRef.afterClosed().subscribe(submitRequest => {
      if (submitRequest) {
        this.deleteComponent(this.createComponentDeleteObj(componentDeleteObj));
      }
    });
  }

  private createComponentDeleteObj(
    componentDeleteObj: ProjectQuickStarterComponentDeleteObj
  ): DeleteComponentRequest {
    return {
      projectKey: this.projectKey,
      quickstarters: [
        {
          component_type: componentDeleteObj.type,
          component_id: componentDeleteObj.id
        }
      ]
    };
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private openNotification(text: string, reload?: boolean) {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = text;
    const dialogRef = this.dialog.open(NotificationComponent, dialogConfig);
    if (reload) {
      dialogRef.afterClosed().subscribe(() => {
        this.triggerLoadProjectData.emit();
      });
    }
  }

  deleteComponent(componentDeleteObj: DeleteComponentRequest) {
    const componentId = componentDeleteObj.quickstarters[0].component_id;
    return this.quickstarterService
      .deleteQuickstarterComponent(componentDeleteObj)
      .subscribe(
        project => {
          this.openNotification(
            `${componentId} successfully deleted, reloading ...`,
            true
          );
        },
        () => {
          this.openNotification(
            `Component could not be deleted, please try again soon`
          );
          return EMPTY;
        }
      );
  }
}
