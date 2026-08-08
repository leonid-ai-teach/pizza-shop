import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AdminAccessEntry } from '../admin.models';
import { ApiError } from '../../core/models/api-error.model';

@Component({
  selector: 'app-admin-admins',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './admin-admins.component.html',
  styleUrl: './admin-admins.component.css',
})
export class AdminAdminsComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly admins = signal<AdminAccessEntry[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
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
    this.adminApi.inviteAdmin(this.form.getRawValue().email).subscribe({
      next: () => {
        this.form.reset({ email: '' });
        this.loadAdmins();
      },
      error: (err: { error?: ApiError }) =>
        this.error.set(err.error?.message ?? 'Der Admin konnte nicht eingeladen werden.'),
    });
  }

  private loadAdmins(): void {
    this.adminApi.getAdmins().subscribe({
      next: (admins) => this.admins.set(admins),
      error: () => this.error.set('Die Admin-Liste konnte nicht geladen werden.'),
    });
  }
}
