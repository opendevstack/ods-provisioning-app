import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {RouterModule} from "@angular/router";
import {ProjectPageComponent} from "./components/project-page.component";

@NgModule({
  declarations: [
    ProjectPageComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([{ path: '', component: ProjectPageComponent }])
  ]
})
export class ProjectPageModule { }
