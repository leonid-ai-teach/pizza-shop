import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PizzaDetailComponent } from './pizza-detail.component';
import { CartService } from '../../cart/cart.service';
import { Pizza } from '../../core/models/pizza.model';

const margherita: Pizza = {
  id: 1,
  name: 'Margherita',
  description: 'Tomato, mozzarella, basil',
  price: 7.5,
  imagePath: null,
  sortOrder: 0,
  toppings: [
    { id: 1, name: 'Extra Käse', description: null, price: 1.0 },
    { id: 2, name: 'Salami', description: null, price: 1.5 },
  ],
};

describe('PizzaDetailComponent', () => {
  let httpMock: HttpTestingController;

  function createComponent() {
    const fixture = TestBed.createComponent(PizzaDetailComponent);
    httpMock.expectOne('/api/pizzas/1').flush(margherita);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    history.replaceState(null, '');
    await TestBed.configureTestingModule({
      imports: [PizzaDetailComponent],
      providers: [
        provideRouter([{ path: 'cart', redirectTo: '' }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } },
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calculates the total price from base price, selected toppings and quantity', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance as unknown as {
      toggleTopping: (id: number) => void;
      setQuantity: (q: number) => void;
      totalPrice: () => number;
    };

    component.toggleTopping(1);
    component.toggleTopping(2);
    component.setQuantity(2);
    fixture.detectChanges();

    expect(component.totalPrice()).toBeCloseTo(20.0);
  });

  it('clamps an emptied or zeroed quantity to 1 instead of desyncing from the shown total', () => {
    const fixture = createComponent();
    const component = fixture.componentInstance as unknown as {
      setQuantity: (q: number) => void;
      quantity: () => number;
      totalPrice: () => number;
    };

    component.setQuantity(3);
    component.setQuantity(Number.NaN);
    expect(component.quantity()).toBe(1);

    component.setQuantity(0);
    expect(component.quantity()).toBe(1);
    expect(component.totalPrice()).toBeCloseTo(7.5);
  });

  it('falls back to adding a new item when the edited cart item no longer exists', () => {
    const cartService = TestBed.inject(CartService);
    history.replaceState({ editItemId: 'item-999', toppingIds: [1], quantity: 4 }, '');

    const fixture = createComponent();
    const component = fixture.componentInstance as unknown as {
      addToCart: () => void;
      isEditing: () => boolean;
    };

    expect(component.isEditing()).toBe(false);
    component.addToCart();

    expect(cartService.items().length).toBe(1);
    expect(cartService.items()[0].quantity).toBe(1);
  });

  it('pre-fills selection from cart edit state and updates the existing cart item', () => {
    const cartService = TestBed.inject(CartService);
    cartService.addItem(margherita, [margherita.toppings[0]], 1);
    const itemId = cartService.items()[0].id;
    history.replaceState(
      { editItemId: itemId, toppingIds: [margherita.toppings[0].id], quantity: 1 },
      '',
    );

    const fixture = createComponent();
    const component = fixture.componentInstance as unknown as {
      addToCart: () => void;
      setQuantity: (q: number) => void;
      toggleTopping: (id: number) => void;
    };

    component.toggleTopping(2);
    component.setQuantity(3);
    component.addToCart();

    expect(cartService.items().length).toBe(1);
    expect(cartService.items()[0].quantity).toBe(3);
    expect(cartService.items()[0].toppings.map((t) => t.id).sort()).toEqual([1, 2]);
  });
});
