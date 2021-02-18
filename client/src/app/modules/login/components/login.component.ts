import { Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { FormBaseComponent } from '../../app-form/components/form-base.component';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthenticationService } from '../../authentication/services/authentication.service';
import { first } from 'rxjs/operators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent extends FormBaseComponent implements OnInit, OnDestroy {
  destroy$ = new Subject<boolean>();

  isLoading: boolean;
  isLoginError = false;

  constructor(
    private renderer: Renderer2,
    private formBuilder: FormBuilder,
    private authenticationService: AuthenticationService,
    private router: Router
  ) {
    super();
  }

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'status-user-auth');
    this.initializeFormGroup();
  }

  intendFormSubmit(): void {
    this.submitButtonClicks++;
    if (this.form.valid) {
      this.initiateLogin();
    }
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  private initiateLogin(): void {
    this.isLoading = true;
    this.authenticationService
      .login(this.form.controls.username.value, this.form.controls.password.value)
      .pipe(first())
      .subscribe(
        () => {
          this.renderer.removeClass(document.body, 'status-user-auth');
          this.router.navigateByUrl('/');
        },
        () => {
          this.isLoading = false;
          this.isLoginError = true;
          return EMPTY;
        }
      );
  }
}
