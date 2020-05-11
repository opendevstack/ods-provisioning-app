import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { FormBuilder, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { EditModeService } from '../../edit-mode/services/edit-mode.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-new-project',
  templateUrl: './new-project.component.html',
  styleUrls: ['./new-project.component.scss']
})
export class NewProjectComponent extends FormBaseComponent
  implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();

  constructor(
    private formBuilder: FormBuilder,
    private editModeService: EditModeService,
    private router: Router
  ) {
    super();
  }

  ngOnInit(): void {
    this.activateEditMode();
    this.initializeFormGroup();
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      name: [null, Validators.required],
      key: [null, Validators.required],
      description: null,
      cdUser: null,
      permissionSet: null,
      adminUser: null,
      adminGroup: null,
      userGroup: null,
      readonlyUserGroup: null,
      projectType: null,
      optInJira: null,
      optInODS: null
    });
  }

  intendFormSubmit(): void {
    this.submitButtonClicks++;
    if (this.form.valid) {
      // initialize loading
      // Call ProjectService to send form data
      // show success or error
    }
  }

  activateEditMode() {
    this.editModeService.emitEditModeFlag({ active: true, context: 'new' });
  }

  deactivateEditMode() {
    this.editModeService.emitEditModeFlag({ active: false, context: 'new' });
  }

  leavePage() {
    this.deactivateEditMode();
    this.router.navigateByUrl('/');
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
