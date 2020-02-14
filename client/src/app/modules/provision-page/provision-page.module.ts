import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ProvisionPageComponent} from "./components/provision-page.component";
import {RouterModule} from "@angular/router";

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild([{ path: '', component: ProvisionPageComponent }])
  ]
})
export class ProvisionPageModule { }
