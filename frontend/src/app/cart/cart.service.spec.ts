import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { Pizza, Topping } from '../core/models/pizza.model';

const margherita: Pizza = {
  id: 1,
  name: 'Margherita',
  description: null,
  price: 7.5,
  imagePath: null,
  sortOrder: 0,
  toppings: [],
};

const extraCheese: Topping = { id: 1, name: 'Extra Käse', description: null, price: 1.0 };
const salami: Topping = { id: 2, name: 'Salami', description: null, price: 1.5 };

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  it('starts empty', () => {
    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
    expect(service.total()).toBe(0);
  });

  it('adds an item and computes total price and item count', () => {
    service.addItem(margherita, [extraCheese, salami], 2);

    expect(service.items().length).toBe(1);
    expect(service.itemCount()).toBe(2);
    expect(service.total()).toBeCloseTo(20.0);
  });

  it('updates quantity for an existing item', () => {
    service.addItem(margherita, [], 1);
    const itemId = service.items()[0].id;

    service.updateQuantity(itemId, 3);

    expect(service.items()[0].quantity).toBe(3);
    expect(service.total()).toBeCloseTo(22.5);
  });

  it('ignores quantity updates below 1', () => {
    service.addItem(margherita, [], 1);
    const itemId = service.items()[0].id;

    service.updateQuantity(itemId, 0);

    expect(service.items()[0].quantity).toBe(1);
  });

  it('removes an item', () => {
    service.addItem(margherita, [], 1);
    const itemId = service.items()[0].id;

    service.removeItem(itemId);

    expect(service.items()).toEqual([]);
  });

  it('replaces toppings and quantity when editing an existing item', () => {
    service.addItem(margherita, [extraCheese], 1);
    const itemId = service.items()[0].id;

    service.updateItem(itemId, [salami], 2);

    expect(service.items()[0].toppings).toEqual([salami]);
    expect(service.items()[0].quantity).toBe(2);
  });

  it('clears all items', () => {
    service.addItem(margherita, [], 1);

    service.clear();

    expect(service.items()).toEqual([]);
  });
});
