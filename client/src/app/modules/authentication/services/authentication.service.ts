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
    let formData: FormData = new FormData();
    formData.append('username', username);
    formData.append('password', password);

    const headers = new HttpHeaders();
    headers.append('Content-Type', 'multipart/form-data');

    return this.httpClient.post<any>(this.apiAuthUrl, formData, { headers: headers });
  }

  logout(): any {
    return this.httpClient.post<any>(this.apiLogoutUrl, '');
  }
}
