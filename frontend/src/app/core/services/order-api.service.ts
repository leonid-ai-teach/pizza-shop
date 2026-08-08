import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateOrderRequest, OrderResponse } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);

  createOrder(request: CreateOrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/orders', request);
  }

  /** Confirmations are addressed by unguessable token, never by the sequential order id. */
  getOrder(publicToken: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/${encodeURIComponent(publicToken)}`);
  }
}
