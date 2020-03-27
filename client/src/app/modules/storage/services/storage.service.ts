import { Inject, Injectable } from '@angular/core';
import { STORAGE_PREFIX } from '../tokens';
import { BrowserService } from '../../browser/services/browser.service';
import { CookieBuilder } from '../../browser/domain/cookie-builder';

@Injectable()
export class StorageService {
  private readonly storage: Storage;
  private keys: Set<string> = new Set();

  private readonly cookieName: string;

  constructor(
    @Inject(STORAGE_PREFIX) private storagePrefix: string,
    private browserService: BrowserService
  ) {
    this.storage = this.browserService.getSessionStorage();
    this.restoreKeys();
    this.cookieName = '_' + this.storagePrefix + 'storage';
  }

  saveItem(key: string, data: object): void {
    const dataString = JSON.stringify(data);
    this.keys.add(key);
    if (this.isStorageWritable()) {
      this.storage.setItem(this.storagePrefix + key, dataString);
    } else {
      const cookieStorage = this.unpackCookieStorage();
      cookieStorage[key] = dataString;
      this.packCookieStorage(cookieStorage);
    }
  }

  getItem(key: string): object | null {
    let dataString: object | null = null;
    if (this.isStorageWritable()) {
      const packedDataString = this.storage.getItem(this.storagePrefix + key);
      if (packedDataString !== undefined) {
        dataString = JSON.parse(packedDataString);
      }
    } else {
      const cookieStorage = this.unpackCookieStorage();
      if (cookieStorage.hasOwnProperty(key)) {
        dataString = cookieStorage[key];
      }
    }
    return dataString === undefined ? null : dataString;
  }

  removeItem(key: string): void {
    if (this.isStorageWritable()) {
      this.storage.removeItem(this.storagePrefix + key);
    } else {
      const cookieStorage = this.unpackCookieStorage();
      if (cookieStorage.hasOwnProperty(key)) {
        delete cookieStorage[key];
        this.packCookieStorage(cookieStorage);
      }
    }
  }

  removeAll(): void {
    if (this.isStorageWritable()) {
      this.keys.forEach((key: string) => {
        this.removeItem(key);
      });
    } else {
      this.browserService.deleteCookieByName(this.cookieName);
    }
    this.keys.clear();
  }

  private restoreKeys(): void {
    if (!this.isStorageWritable()) {
      return;
    }
    Object.keys(this.storage).forEach(key => {
      if (typeof this.storage[key] !== 'function') {
        this.keys.add(key.replace(this.storagePrefix, ''));
      }
    });
  }

  // There should always be a check in place is storage is writable or not, see
  // https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API/Using_the_Web_Storage_API
  private isStorageWritable(): boolean {
    try {
      this.storage.setItem(this.storagePrefix + '__check', 'test');
      this.storage.removeItem(this.storagePrefix + '__check');
      return true;
    } catch (exception) {
      return false;
    }
  }

  private unpackCookieStorage(): { [s: string]: any } {
    const cookie = this.browserService.getCookie(this.cookieName);
    return cookie ? cookie.getValueAsObject() : {};
  }

  private packCookieStorage(unpackedCookieStorage: {
    [s: string]: string;
  }): void {
    const cookie = new CookieBuilder(this.cookieName)
      .withObjectValue(unpackedCookieStorage)
      .build();
    this.browserService.setCookie(cookie);
  }
}
