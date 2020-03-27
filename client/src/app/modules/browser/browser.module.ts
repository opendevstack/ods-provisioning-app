import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserService } from './services/browser.service';

@NgModule({
  imports: [CommonModule],
  declarations: [],
  providers: [BrowserService]
})
export class BrowserModule {}
