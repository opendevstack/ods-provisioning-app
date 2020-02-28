export class Cookie {
  readonly name: string;
  readonly value: string;
  readonly secure: boolean;
  readonly path: string;
  readonly maxAge: number;

  constructor(
    name: string,
    value: string,
    secure: boolean,
    maxAge: number,
    path: string
  ) {
    this.name = name;
    this.value = value;
    this.secure = secure;
    this.maxAge = maxAge;
    this.path = path;
  }

  getValueAsObject(): any {
    return JSON.parse(this.value);
  }

  buildCookieString(): string {
    let cookieString = `${encodeURIComponent(this.name)}=`;
    cookieString += `${encodeURIComponent(this.value)}`;
    cookieString += `;path=${this.path}`;
    if (this.secure) {
      cookieString += `;secure`;
    }
    if (this.maxAge) {
      cookieString += `;max-age=${this.maxAge}`;
    }
    return cookieString;
  }
}
