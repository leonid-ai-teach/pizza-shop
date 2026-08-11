import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PizzaApiService } from '../../core/services/pizza-api.service';
import { Pizza } from '../../core/models/pizza.model';

@Component({
  selector: 'app-menu',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
})
export class MenuComponent {
  private readonly pizzaApi = inject(PizzaApiService);

  protected readonly pizzas = signal<Pizza[]>([]);
  protected readonly loadError = signal(false);

  constructor() {
    this.pizzaApi.getPizzas().subscribe({
      next: (pizzas) => this.pizzas.set(pizzas),
      error: () => this.loadError.set(true),
    });
  }

  /**
   * Gerichtnummer, wie man sie in der Pizzeria bestellt ("einmal die 42"). Bewusst aus
   * der ID abgeleitet und nicht aus der Listenposition: eine Nummer taugt nur etwas,
   * wenn sie stabil bleibt, wenn ein Gericht aus der Karte genommen wird.
   */
  protected dishNumber(pizza: Pizza): string {
    return String(pizza.id).padStart(2, '0');
  }
}
