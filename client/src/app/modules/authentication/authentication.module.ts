import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthenticationService } from './services/authentication.service';
import { HttpClientModule } from '@angular/common/http';
import { API_AUTH_URL, API_LOGOUT_URL } from '../../tokens';

@NgModule({
  declarations: [],
  imports: [CommonModule, HttpClientModule],
  providers: [AuthenticationService]
})
export class AuthenticationModule {
  static withOptions(options: { apiAuthUrl: string; apiLogoutUrl: string }): ModuleWithProviders<AuthenticationModule> {
    return {
      ngModule: AuthenticationModule,
      providers: [
        {
          provide: API_AUTH_URL,
          useValue: options.apiAuthUrl
        },
        {
          provide: API_LOGOUT_URL,
          useValue: options.apiLogoutUrl
        }
      ]
    };
  }
}
