import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pizza, Topping } from '../models/pizza.model';

@Injectable({ providedIn: 'root' })
export class PizzaApiService {
  private readonly http = inject(HttpClient);

  getPizzas(): Observable<Pizza[]> {
    return this.http.get<Pizza[]>('/api/pizzas');
  }

  getPizza(id: number): Observable<Pizza> {
    return this.http.get<Pizza>(`/api/pizzas/${id}`);
  }

  getToppings(): Observable<Topping[]> {
    return this.http.get<Topping[]>('/api/toppings');
  }
}
