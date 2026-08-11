import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PizzaApiService } from '../../core/services/pizza-api.service';
import { Pizza, Topping } from '../../core/models/pizza.model';
import { CartService } from '../../cart/cart.service';
import { calculateUnitPrice } from '../../cart/cart-item.model';

@Component({
  selector: 'app-pizza-detail',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './pizza-detail.component.html',
  styleUrl: './pizza-detail.component.css',
})
export class PizzaDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pizzaApi = inject(PizzaApiService);
  private readonly cartService = inject(CartService);

  protected readonly pizza = signal<Pizza | null>(null);
  protected readonly loadError = signal(false);
  protected readonly selectedToppingIds = signal<number[]>([]);
  protected readonly quantity = signal(1);
  private readonly editItemId = signal<string | null>(null);

  protected readonly selectedToppings = computed<Topping[]>(() => {
    const pizza = this.pizza();
    if (!pizza) {
      return [];
    }
    const selectedIds = this.selectedToppingIds();
    return pizza.toppings.filter((topping) => selectedIds.includes(topping.id));
  });

  protected readonly unitPrice = computed(() => {
    const pizza = this.pizza();
    return pizza ? calculateUnitPrice(pizza.price, this.selectedToppings()) : 0;
  });

  protected readonly totalPrice = computed(() => this.unitPrice() * this.quantity());
  protected readonly isEditing = computed(() => this.editItemId() !== null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.pizzaApi.getPizza(id).subscribe({
      next: (pizza) => this.pizza.set(pizza),
      error: () => this.loadError.set(true),
    });

    const editState = history.state as
      | { editItemId?: string; toppingIds?: number[]; quantity?: number }
      | undefined;
    // history.state survives a page reload but the in-memory cart does not, so only
    // enter edit mode if the referenced item is actually still in the cart.
    if (editState?.editItemId && this.cartService.hasItem(editState.editItemId)) {
      this.editItemId.set(editState.editItemId);
      this.selectedToppingIds.set(editState.toppingIds ?? []);
      this.quantity.set(editState.quantity ?? 1);
    }
  }

  /** Gerichtnummer wie auf der Karte - siehe MenuComponent.dishNumber. */
  protected dishNumber(pizza: Pizza): string {
    return String(pizza.id).padStart(2, '0');
  }

  protected isSelected(toppingId: number): boolean {
    return this.selectedToppingIds().includes(toppingId);
  }

  protected toggleTopping(toppingId: number): void {
    this.selectedToppingIds.update((ids) =>
      ids.includes(toppingId) ? ids.filter((id) => id !== toppingId) : [...ids, toppingId],
    );
  }

  /** Clamps to at least 1 so an emptied or zeroed input can't desync from the shown total. */
  protected setQuantity(quantity: number): void {
    this.quantity.set(Number.isFinite(quantity) && quantity >= 1 ? Math.floor(quantity) : 1);
  }

  /** Re-syncs the native input after clamping, since [value] alone won't repaint an unchanged signal. */
  protected syncQuantityInput(input: HTMLInputElement): void {
    input.value = String(this.quantity());
  }

  protected addToCart(): void {
    const pizza = this.pizza();
    if (!pizza) {
      return;
    }
    const editingItemId = this.editItemId();
    if (editingItemId) {
      this.cartService.updateItem(editingItemId, this.selectedToppings(), this.quantity());
    } else {
      this.cartService.addItem(pizza, this.selectedToppings(), this.quantity());
    }
    this.router.navigate(['/cart']);
  }
}
