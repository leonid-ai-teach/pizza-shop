import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { CartPageComponent } from './cart-page.component';
import { CartService } from '../cart.service';
import { Pizza, Topping } from '../../core/models/pizza.model';

registerLocaleData(localeDe);

const margherita: Pizza = {
  id: 1,
  name: 'Margherita',
  description: null,
  price: 7.5,
  imagePath: null,
  sortOrder: 0,
  toppings: [],
};

const salami: Pizza = { ...margherita, id: 2, name: 'Salami', price: 8.5 };

const extraCheese: Topping = { id: 1, name: 'Extra Käse', description: null, price: 1.0 };

describe('CartPageComponent', () => {
  let cartService: CartService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartPageComponent],
      providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'de-DE' }],
    }).compileComponents();
    cartService = TestBed.inject(CartService);
  });

  function render() {
    const fixture = TestBed.createComponent(CartPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('shows an empty-cart message when there are no items', () => {
    const fixture = render();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Noch nichts ausgewählt');
    expect(fixture.nativeElement.querySelector('[data-testid="checkout-button"]')).toBeNull();
  });

  it('renders cart items with quantity, toppings and running total', () => {
    cartService.addItem(margherita, [extraCheese], 2);

    const fixture = render();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Margherita');
    expect(text).toContain('Extra Käse');
    expect(text).toContain('17,00');
  });

  it('increasing quantity updates the item and the total', () => {
    cartService.addItem(margherita, [], 1);
    const itemId = cartService.items()[0].id;
    const fixture = render();

    const increaseButton = fixture.nativeElement.querySelector(
      `[data-testid="increase-quantity-${itemId}"]`,
    ) as HTMLButtonElement;
    increaseButton.click();
    fixture.detectChanges();

    expect(cartService.items()[0].quantity).toBe(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('15,00');
  });

  it('removing an item empties the cart view', () => {
    cartService.addItem(margherita, [], 1);
    const itemId = cartService.items()[0].id;
    const fixture = render();

    const removeButton = fixture.nativeElement.querySelector(
      `[data-testid="remove-item-${itemId}"]`,
    ) as HTMLButtonElement;
    removeButton.click();
    fixture.detectChanges();

    expect(cartService.items().length).toBe(0);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Noch nichts ausgewählt');
  });

  it('gives each cart row its own action test ids so rows stay individually addressable', () => {
    cartService.addItem(margherita, [], 1);
    cartService.addItem(salami, [], 1);
    const fixture = render();

    const removeButtons = fixture.nativeElement.querySelectorAll('[data-testid^="remove-item-"]');
    const testIds = Array.from(removeButtons).map((el) =>
      (el as HTMLElement).getAttribute('data-testid'),
    );

    expect(testIds.length).toBe(2);
    expect(new Set(testIds).size).toBe(2);
  });

  it('removes only the targeted row when several items are in the cart', () => {
    cartService.addItem(margherita, [], 1);
    cartService.addItem(salami, [], 1);
    const salamiItemId = cartService.items()[1].id;
    const fixture = render();

    (
      fixture.nativeElement.querySelector(
        `[data-testid="remove-item-${salamiItemId}"]`,
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    expect(cartService.items().map((i) => i.pizza.name)).toEqual(['Margherita']);
  });

  it('navigates to checkout when the cart has items', () => {
    cartService.addItem(margherita, [], 1);
    const fixture = render();
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    const checkoutButton = fixture.nativeElement.querySelector(
      '[data-testid="checkout-button"]',
    ) as HTMLButtonElement;
    checkoutButton.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/checkout']);
  });
});
