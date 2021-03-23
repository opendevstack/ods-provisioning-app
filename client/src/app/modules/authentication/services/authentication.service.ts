import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_AUTH_URL, API_LOGOUT_URL } from '../../../tokens';
import { ActivatedRoute, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private _sso = false;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    @Inject(API_AUTH_URL) private apiAuthUrl: string,
    @Inject(API_LOGOUT_URL) private apiLogoutUrl: string
  ) {}

  get sso(): boolean {
    return this._sso;
  }

  checkAndSetSsoMode() {
    this.activatedRoute.queryParams.subscribe(params => {
      if (params['sso']) {
        this._sso = true;
        this.removeSsoUrlParam();
      }
    });
  }

  login(username: string, password: string): any {
    const formData: FormData = new FormData();
    formData.append('username', username);
    formData.append('password', password);

    const headers = new HttpHeaders();
    headers.append('Content-Type', 'multipart/form-data');

    return this.httpClient.post<any>(this.apiAuthUrl, formData, { headers: headers });
  }

  logout(): any {
    return this.httpClient.get<any>(this.apiLogoutUrl);
  }

  private removeSsoUrlParam() {
    this.router.navigate([], {
      queryParams: {
        sso: null
      }
    });
  }
}
