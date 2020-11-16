import { Cookie } from './cookie';

export class CookieBuilder {
  protected settings: {
    name: string;
    value: string;
    secure: boolean;
    maxAge: number;
    path: string;
  };

  constructor(cookieName: string) {
    this.settings = {
      name: cookieName,
      value: '',
      secure: false,
      maxAge: null,
      path: '/'
    };
  }

  build(): Cookie {
    return new Cookie(this.settings.name, this.settings.value, this.settings.secure, this.settings.maxAge, this.settings.path);
  }

  withStringValue(value: string): this {
    this.settings.value = value;
    return this;
  }

  withObjectValue(value: object): this {
    this.settings.value = JSON.stringify(value);
    return this;
  }

  asSecure(): this {
    this.settings.secure = true;
    return this;
  }

  withExpiringInDays(daysTillExpiring: number): this {
    this.settings.maxAge = daysTillExpiring * 86400;
    return this;
  }
}
