import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {LoadingIndicatorComponent} from "./components/loading-indicator.component";
import {MatProgressBarModule} from "@angular/material/progress-bar";



@NgModule({
  declarations: [LoadingIndicatorComponent],
  imports: [
    CommonModule,
    MatProgressBarModule
  ],
  exports: [
    LoadingIndicatorComponent
  ]
})
export class LoadingIndicatorModule { }
