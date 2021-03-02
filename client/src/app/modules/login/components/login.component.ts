import { Component, OnInit, Renderer2 } from '@angular/core';
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
export class LoginComponent extends FormBaseComponent implements OnInit {
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

  private initializeFormGroup(): void {
    this.form = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  private initiateLogin(): void {
    const [username, password] = [this.form.controls.username.value, this.form.controls.password.value];
    this.isLoading = true;
    this.authenticationService
      .login(username, password)
      .pipe(first())
      .subscribe(
        () => {
          this.form.reset();
          this.renderer.removeClass(document.body, 'status-user-auth');
          this.router.navigateByUrl('/');
        },
        () => {
          this.isLoading = false;
          this.isLoginError = true;
        }
      );
  }
}
