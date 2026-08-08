import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminApiService } from '../admin-api.service';
import { OrderSummary } from '../admin.models';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
export class AdminDashboardComponent {
  private readonly adminApi = inject(AdminApiService);

  protected readonly summary = signal<OrderSummary | null>(null);
  protected readonly loadError = signal(false);

  constructor() {
    this.adminApi.getOrderSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => this.loadError.set(true),
    });
  }
}
