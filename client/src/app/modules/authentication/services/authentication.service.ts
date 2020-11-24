import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_AUTH_URL, API_LOGOUT_URL } from '../../../tokens';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private _sso = false;

  constructor(
    private httpClient: HttpClient,
    @Inject(API_AUTH_URL) private apiAuthUrl: string,
    @Inject(API_LOGOUT_URL) private apiLogoutUrl: string
  ) {}

  set sso(enabled: boolean) {
    this._sso = enabled;
  }

  get sso(): boolean {
    return this._sso;
  }

  login(username: string, password: string): any {
    const authData = window.btoa(username + ':' + password);
    const httpOptions = {
      headers: new HttpHeaders({ Authorization: `Basic ${authData}` })
    };
    return this.httpClient.get<any>(this.apiAuthUrl, httpOptions);
  }

  logout(): any {
    return this.httpClient.post<any>(this.apiLogoutUrl, '');
  }
}
