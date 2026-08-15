import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AdminAccessEntry } from '../admin.models';
import { ApiError } from '../../core/models/api-error.model';

/** Matches the minimum the backend enforces on InviteAdminRequest and ChangePasswordRequest. */
const PASSWORT_MINDESTLAENGE = 10;

@Component({
  selector: 'app-admin-admins',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './admin-admins.component.html',
  styleUrl: './admin-admins.component.css',
})
export class AdminAdminsComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly mindestlaenge = PASSWORT_MINDESTLAENGE;

  protected readonly admins = signal<AdminAccessEntry[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(PASSWORT_MINDESTLAENGE)]],
  });

  protected readonly passwordError = signal<string | null>(null);
  protected readonly passwordChanged = signal(false);

  protected readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(PASSWORT_MINDESTLAENGE)]],
  });

  constructor() {
    this.loadAdmins();
  }

  protected invite(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.adminApi.inviteAdmin(email, password).subscribe({
      next: () => {
        this.form.reset({ email: '', password: '' });
        this.loadAdmins();
      },
      error: (err: { error?: ApiError }) =>
        this.error.set(err.error?.message ?? 'Der Admin konnte nicht eingeladen werden.'),
    });
  }

  protected changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.passwordError.set(null);
    this.passwordChanged.set(false);

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.adminApi.changeOwnPassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.passwordForm.reset({ currentPassword: '', newPassword: '' });
        this.passwordChanged.set(true);
      },
      error: (err: { error?: ApiError }) =>
        this.passwordError.set(
          err.error?.message ?? 'Das Passwort konnte nicht geändert werden.',
        ),
    });
  }

  private loadAdmins(): void {
    this.adminApi.getAdmins().subscribe({
      next: (admins) => this.admins.set(admins),
      error: () => this.error.set('Die Admin-Liste konnte nicht geladen werden.'),
    });
  }
}
