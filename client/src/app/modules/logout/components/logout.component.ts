import { Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { BrowserService } from '../../browser/services/browser.service';
import { Subject } from 'rxjs';
import { AuthenticationService } from '../../authentication/services/authentication.service';

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html'
})
export class LogoutComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();
  isLoading = true;

  constructor(private browserService: BrowserService, private renderer: Renderer2, private authenticationService: AuthenticationService) {}

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'status-user-auth');
    this.authenticationService.logout().subscribe(() => {
      this.isLoading = false;
    });
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
