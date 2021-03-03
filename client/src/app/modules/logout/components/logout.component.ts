import { Component, OnInit, Renderer2 } from '@angular/core';
import { BrowserService } from '../../browser/services/browser.service';
import { AuthenticationService } from '../../authentication/services/authentication.service';
import { first } from 'rxjs/operators';

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html'
})
export class LogoutComponent implements OnInit {
  isLoading = true;

  constructor(private browserService: BrowserService, private renderer: Renderer2, private authenticationService: AuthenticationService) {}

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'status-user-auth');
    this.authenticationService
      .logout()
      .pipe(first())
      .subscribe(() => {
        this.isLoading = false;
      });
  }
}
