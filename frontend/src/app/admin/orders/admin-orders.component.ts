import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminApiService } from '../admin-api.service';
import { OrderResponse, OrderStatus } from '../../core/models/order.model';
import { ORDER_STATUS_LABELS, ORDER_TYPE_LABELS } from '../../core/models/order-labels';

/** Legal next statuses, mirroring the backend state machine so the UI never offers a dead action. */
const ALLOWED_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  NEW: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['DONE', 'CANCELLED'],
  DONE: [],
  CANCELLED: [],
};

@Component({
  selector: 'app-admin-orders',
  imports: [DecimalPipe],
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.css',
})
export class AdminOrdersComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly orders = signal<OrderResponse[]>([]);
  protected readonly selectedOrder = signal<OrderResponse | null>(null);
  protected readonly statusFilter = signal<OrderStatus | ''>('');
  protected readonly error = signal<string | null>(null);

  protected readonly statuses: OrderStatus[] = ['NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

  protected readonly selectedOrderTransitions = computed(() => {
    const order = this.selectedOrder();
    return order ? ALLOWED_TRANSITIONS[order.status] : [];
  });

  constructor() {
    const status = this.route.snapshot.queryParamMap.get('status') as OrderStatus | null;
    this.statusFilter.set(status ?? '');
    this.loadOrders();
  }

  protected statusLabel(status: OrderStatus): string {
    return ORDER_STATUS_LABELS[status] ?? status;
  }

  protected orderTypeLabel(orderType: OrderResponse['orderType']): string {
    return ORDER_TYPE_LABELS[orderType] ?? orderType;
  }

  protected applyFilter(status: string): void {
    this.statusFilter.set(status as OrderStatus | '');
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: status ? { status } : {},
    });
    this.loadOrders();
  }

  protected selectOrder(order: OrderResponse): void {
    this.adminApi.getOrder(order.id).subscribe({
      next: (detail) => this.selectedOrder.set(detail),
      error: () => this.error.set('Die Bestellung konnte nicht geladen werden.'),
    });
  }

  protected changeStatus(order: OrderResponse, status: OrderStatus): void {
    this.error.set(null);
    this.adminApi.updateOrderStatus(order.id, status).subscribe({
      next: (updated) => {
        this.selectedOrder.set(updated);
        this.loadOrders();
      },
      error: () => this.error.set('Der Status konnte nicht geändert werden.'),
    });
  }

  private loadOrders(): void {
    const filter = this.statusFilter();
    this.adminApi.getOrders(filter || undefined).subscribe({
      next: (orders) => this.orders.set(orders),
      error: () => this.error.set('Die Bestellungen konnten nicht geladen werden.'),
    });
  }
}
