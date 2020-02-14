import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {RouterModule} from "@angular/router";
import {ProjectsPageComponent} from "./components/projects-page.component";

@NgModule({
  declarations: [
    ProjectsPageComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([{ path: '', component: ProjectsPageComponent }])
  ]
})
export class ProjectsPageModule { }
