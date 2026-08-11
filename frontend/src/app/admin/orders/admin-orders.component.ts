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

/**
 * Auf einem Knopf steht, was er tut - nicht, wie der Zustand danach heisst.
 * "Storniert" als Knopfaufschrift liest sich wie eine Meldung, "Stornieren"
 * wie eine Handlung.
 */
const STATUS_AKTIONEN: Record<OrderStatus, string> = {
  NEW: 'Zurück auf neu',
  IN_PROGRESS: 'In Bearbeitung nehmen',
  DONE: 'Fertigstellen',
  CANCELLED: 'Stornieren',
};

const STATUS_KLASSEN: Record<OrderStatus, string> = {
  NEW: 'status-neu',
  IN_PROGRESS: 'status-arbeit',
  DONE: 'status-fertig',
  CANCELLED: 'status-storniert',
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

  protected statusAktion(status: OrderStatus): string {
    return STATUS_AKTIONEN[status] ?? status;
  }

  /** Klassen des Statusstempels - die Farbe meldet, ob noch etwas zu tun ist. */
  protected statusStempel(status: OrderStatus): string {
    return `status ${STATUS_KLASSEN[status]}`;
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
