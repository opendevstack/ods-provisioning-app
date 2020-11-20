import { Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { Subject } from 'rxjs';
import { FormBaseComponent } from '../../app-form/components/form-base.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent extends FormBaseComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();

  constructor(private renderer: Renderer2) {
    super();
  }

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'status-user-auth');
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
