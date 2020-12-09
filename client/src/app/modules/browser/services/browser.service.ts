import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { CookieBuilder } from '../domain/cookie-builder';
import { Cookie } from '../domain/cookie';

@Injectable()
export class BrowserService {
  private document: Document;
  private window: Window;

  constructor(@Inject(DOCUMENT) doc: any) {
    this.document = doc as Document;
    this.window = this.document.defaultView;
  }

  getSessionStorage(): Storage {
    return this.window.sessionStorage;
  }

  getLocalStorage(): Storage {
    return this.window.localStorage;
  }

  setCookie(cookie: Cookie): void {
    this.document.cookie = cookie.buildCookieString();
  }

  deleteCookieByName(name: string): void {
    this.document.cookie = `${encodeURIComponent(name)}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
  }

  getCookie(cookieName: string): Cookie | null {
    const cookies = this.readCookies();
    if (cookies.hasOwnProperty(cookieName)) {
      return new CookieBuilder(cookieName).withStringValue(cookies[cookieName]).build();
    }
    return null;
  }

  private readCookies(): { [s: string]: string } {
    const cookies: { [s: string]: string } = {};

    if (this.document.cookie.length > 0) {
      const pairs: string[] = this.document.cookie.split(';');

      pairs.forEach(item => {
        if (item.length > 0) {
          const pair = item.split('=');
          cookies[decodeURIComponent(pair[0].trim())] = decodeURIComponent(pair[1]);
        }
      });
    }

    return cookies;
  }

  scrollIntoViewById(elementName: string): void {
    const element = this.document.getElementById(elementName);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
