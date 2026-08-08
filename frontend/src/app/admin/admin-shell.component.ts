import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AdminApiService } from './admin-api.service';

@Component({
  selector: 'app-admin-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.css',
})
export class AdminShellComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly router = inject(Router);

  protected readonly adminEmail = signal<string | null>(null);

  constructor() {
    this.adminApi.me().subscribe({
      next: (admin) => this.adminEmail.set(admin.email),
      error: () => this.adminEmail.set(null),
    });
  }

  protected logout(): void {
    this.adminApi.logout().subscribe({
      next: () => this.router.navigate(['/admin/login']),
      error: () => this.router.navigate(['/admin/login']),
    });
  }
}
