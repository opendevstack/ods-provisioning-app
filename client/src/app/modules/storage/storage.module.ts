import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { STORAGE_PREFIX } from './tokens';
import { BrowserModule } from '../browser/browser.module';
import { StorageService } from './services/storage.service';

@NgModule({
  imports: [CommonModule, BrowserModule],
  declarations: [],
  providers: [StorageService]
})
export class StorageModule {
  static withOptions(options: {
    storagePrefix: string;
  }): ModuleWithProviders<StorageModule> {
    return {
      ngModule: StorageModule,
      providers: [
        {
          provide: STORAGE_PREFIX,
          useValue: options.storagePrefix
        }
      ]
    };
  }
}
