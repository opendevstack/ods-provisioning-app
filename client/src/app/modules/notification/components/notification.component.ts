import { Component, Inject, OnInit } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogRef
} from '@angular/material/dialog';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html'
})
export class NotificationComponent implements OnInit {
  notificationText: string;

  constructor(
    public dialog: MatDialog,
    private dialogRef: MatDialogRef<NotificationComponent>,
    @Inject(MAT_DIALOG_DATA) data
  ) {
    this.notificationText = data;
  }

  ngOnInit(): void {
    setTimeout(() => this.dialogRef.close(), 1000);

    this.dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
    });
  }
}
