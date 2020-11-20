import { Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { BrowserService } from '../../browser/services/browser.service';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html'
})
export class LogoutComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();

  constructor(private browserService: BrowserService, private renderer: Renderer2) {}

  ngOnInit(): void {
    this.browserService.deleteCookieByName('JSESSIONID');
    this.renderer.addClass(document.body, 'status-user-auth');
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
