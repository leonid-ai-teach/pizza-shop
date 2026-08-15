import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminApiService } from '../admin-api.service';

@Component({
  selector: 'app-admin-login',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.css',
})
export class AdminLoginComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly loginFailed = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loginFailed.set(false);

    const { email, password } = this.form.getRawValue();
    this.adminApi.login(email, password).subscribe({
      next: () => void this.router.navigate(['/admin']),
      // Whatever went wrong, the visitor can only try again — and the server deliberately does
      // not say whether the email or the password was the problem.
      error: () => {
        this.loginFailed.set(true);
        // reset() rather than patchValue(''): the latter leaves the control touched, so the
        // now-empty field would add "Bitte das Passwort eingeben" underneath the actual error.
        this.form.controls.password.reset('');
      },
    });
  }
}
