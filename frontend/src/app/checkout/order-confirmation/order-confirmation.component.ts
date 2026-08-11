import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderApiService } from '../../core/services/order-api.service';
import { OrderResponse, OrderStatus, OrderType } from '../../core/models/order.model';
import { ORDER_STATUS_LABELS, ORDER_TYPE_LABELS } from '../../core/models/order-labels';

@Component({
  selector: 'app-order-confirmation',
  imports: [DecimalPipe, DatePipe, RouterLink],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.css',
})
export class OrderConfirmationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApiService);

  protected readonly order = signal<OrderResponse | null>(null);
  protected readonly loadError = signal(false);

  constructor() {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    this.orderApi.getOrder(token).subscribe({
      next: (order) => this.order.set(order),
      error: () => this.loadError.set(true),
    });
  }

  protected statusLabel(status: OrderStatus): string {
    return ORDER_STATUS_LABELS[status] ?? status;
  }

  protected orderTypeLabel(orderType: OrderType): string {
    return ORDER_TYPE_LABELS[orderType] ?? orderType;
  }
}
