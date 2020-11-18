import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AppRoutingModule } from './modules/app-routing/app-routing.module';
import { SidebarModule } from './modules/sidebar/sidebar.module';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@NgModule({
  declarations: [AppComponent],
  imports: [BrowserModule, BrowserAnimationsModule, AppRoutingModule, SidebarModule, MatButtonModule, MatIconModule],
  bootstrap: [AppComponent]
})
export class AppModule {}
