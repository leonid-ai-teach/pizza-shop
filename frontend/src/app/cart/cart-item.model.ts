import { Pizza, Topping } from '../core/models/pizza.model';

export interface CartItem {
  id: string;
  pizza: Pizza;
  toppings: Topping[];
  quantity: number;
}

/**
 * Single source of truth for the position price formula from the master prompt (§6):
 * (base price + sum of topping prices) x quantity. The server recalculates this
 * independently on order creation; this is only for what the customer sees while
 * configuring.
 */
export function calculateUnitPrice(basePrice: number, toppings: Topping[]): number {
  return toppings.reduce((sum, topping) => sum + topping.price, basePrice);
}

export function unitPrice(item: CartItem): number {
  return calculateUnitPrice(item.pizza.price, item.toppings);
}

export function lineTotal(item: CartItem): number {
  return unitPrice(item) * item.quantity;
}
