import { Injectable, computed, signal } from '@angular/core';
import { Pizza, Topping } from '../core/models/pizza.model';
import { CartItem, lineTotal } from './cart-item.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<CartItem[]>([]);
  private nextId = 0;

  readonly items = this._items.asReadonly();

  readonly itemCount = computed(() =>
    this._items().reduce((sum, item) => sum + item.quantity, 0),
  );

  readonly total = computed(() =>
    this._items().reduce((sum, item) => sum + lineTotal(item), 0),
  );

  addItem(pizza: Pizza, toppings: Topping[], quantity: number): void {
    const item: CartItem = {
      id: this.nextItemId(),
      pizza,
      toppings,
      quantity,
    };
    this._items.update((items) => [...items, item]);
  }

  hasItem(itemId: string): boolean {
    return this._items().some((item) => item.id === itemId);
  }

  updateItem(itemId: string, toppings: Topping[], quantity: number): void {
    this._items.update((items) =>
      items.map((item) => (item.id === itemId ? { ...item, toppings, quantity } : item)),
    );
  }

  updateQuantity(itemId: string, quantity: number): void {
    if (quantity < 1) {
      return;
    }
    this._items.update((items) =>
      items.map((item) => (item.id === itemId ? { ...item, quantity } : item)),
    );
  }

  removeItem(itemId: string): void {
    this._items.update((items) => items.filter((item) => item.id !== itemId));
  }

  clear(): void {
    this._items.set([]);
  }

  // A plain counter rather than crypto.randomUUID(): ids only need to be unique
  // within one in-memory cart, and randomUUID is unavailable outside secure
  // contexts (e.g. reaching a dev server over plain HTTP from a phone).
  private nextItemId(): string {
    this.nextId += 1;
    return `item-${this.nextId}`;
  }
}
