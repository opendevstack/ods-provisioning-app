import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AboutPageComponent } from './components/about-page.component';

@NgModule({
  imports: [CommonModule, RouterModule.forChild([{ path: '', component: AboutPageComponent }])]
})
export class AboutPageModule {}
