import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OrderResponse, OrderStatus } from '../core/models/order.model';
import {
  AdminAccessEntry,
  AdminPizza,
  AdminTopping,
  CurrentAdmin,
  OrderSummary,
  PizzaWriteRequest,
  ToppingWriteRequest,
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  login(email: string, password: string): Observable<void> {
    return this.http.post<void>('/api/admin/login', { email, password });
  }

  me(): Observable<CurrentAdmin> {
    return this.http.get<CurrentAdmin>('/api/admin/me');
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/admin/logout', {});
  }

  changeOwnPassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/admin/me/password', { currentPassword, newPassword });
  }

  getOrderSummary(): Observable<OrderSummary> {
    return this.http.get<OrderSummary>('/api/admin/orders/summary');
  }

  getOrders(status?: OrderStatus): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>('/api/admin/orders', {
      params: status ? { status } : {},
    });
  }

  getOrder(id: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/admin/orders/${id}`);
  }

  updateOrderStatus(id: number, status: OrderStatus): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/admin/orders/${id}/status`, { status });
  }

  getPizzas(): Observable<AdminPizza[]> {
    return this.http.get<AdminPizza[]>('/api/admin/pizzas');
  }

  createPizza(request: PizzaWriteRequest): Observable<AdminPizza> {
    return this.http.post<AdminPizza>('/api/admin/pizzas', request);
  }

  updatePizza(id: number, request: PizzaWriteRequest): Observable<AdminPizza> {
    return this.http.put<AdminPizza>(`/api/admin/pizzas/${id}`, request);
  }

  setPizzaActive(id: number, active: boolean): Observable<AdminPizza> {
    return this.http.patch<AdminPizza>(`/api/admin/pizzas/${id}/active`, { active });
  }

  getToppings(): Observable<AdminTopping[]> {
    return this.http.get<AdminTopping[]>('/api/admin/toppings');
  }

  createTopping(request: ToppingWriteRequest): Observable<AdminTopping> {
    return this.http.post<AdminTopping>('/api/admin/toppings', request);
  }

  updateTopping(id: number, request: ToppingWriteRequest): Observable<AdminTopping> {
    return this.http.put<AdminTopping>(`/api/admin/toppings/${id}`, request);
  }

  setToppingActive(id: number, active: boolean): Observable<AdminTopping> {
    return this.http.patch<AdminTopping>(`/api/admin/toppings/${id}/active`, { active });
  }

  getAdmins(): Observable<AdminAccessEntry[]> {
    return this.http.get<AdminAccessEntry[]>('/api/admin/admins');
  }

  inviteAdmin(email: string, password: string): Observable<AdminAccessEntry> {
    return this.http.post<AdminAccessEntry>('/api/admin/admins', { email, password });
  }
}
