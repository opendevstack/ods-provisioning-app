import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ConfirmationConfig } from '../domain/confirmation-config';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-confirmation',
  templateUrl: './confirmation.component.html'
})
export class ConfirmationComponent implements OnInit {
  confirmationConfig: ConfirmationConfig;
  form: FormGroup;

  constructor(
    public dialog: MatDialog,
    private dialogRef: MatDialogRef<ConfirmationComponent>,
    private formBuilder: FormBuilder,
    @Inject(MAT_DIALOG_DATA) data
  ) {
    this.confirmationConfig = data;
  }

  ngOnInit() {
    if (this.confirmationConfig.verify) {
      this.form = this.formBuilder.group({
        verify: ['', [Validators.required]]
      });
    }
  }
}
