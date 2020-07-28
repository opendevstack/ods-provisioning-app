import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { ReactiveFormsModule } from '@angular/forms';
import { SidebarComponent } from './components/sidebar.component';
import { MatListModule } from '@angular/material/list';
import { RouterModule } from '@angular/router';
import { LoadingIndicatorModule } from '../loading-indicator/loading-indicator.module';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AppFormModule } from '../app-form/app-form.module';

@NgModule({
  declarations: [SidebarComponent],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    AppFormModule,
    LoadingIndicatorModule,
    MatListModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule
  ],
  exports: [SidebarComponent]
})
export class SidebarModule {}
