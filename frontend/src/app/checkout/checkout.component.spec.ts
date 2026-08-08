import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { CheckoutComponent } from './checkout.component';
import { CartService } from '../cart/cart.service';
import { Pizza } from '../core/models/pizza.model';
import { OrderResponse } from '../core/models/order.model';

const margherita: Pizza = {
  id: 1,
  name: 'Margherita',
  description: null,
  price: 7.5,
  imagePath: null,
  sortOrder: 0,
  toppings: [],
};

function fillRequiredContactFields(fixture: ReturnType<typeof TestBed.createComponent>) {
  const el = fixture.nativeElement as HTMLElement;
  setInput(el, 'firstName', 'Mario');
  setInput(el, 'lastName', 'Rossi');
  setInput(el, 'phone', '0123456789');
  setInput(el, 'email', 'mario@example.com');
}

function setInput(root: HTMLElement, testId: string, value: string) {
  const input = root.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

describe('CheckoutComponent', () => {
  let httpMock: HttpTestingController;
  let cartService: CartService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        provideRouter([{ path: 'cart', redirectTo: '' }, { path: 'order-confirmation/:token', redirectTo: '' }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    cartService = TestBed.inject(CartService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('redirects to the cart when there is nothing to check out', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    TestBed.createComponent(CheckoutComponent);

    expect(navigateSpy).toHaveBeenCalledWith(['/cart']);
  });

  it('hides address fields for pickup by default', () => {
    cartService.addItem(margherita, [], 1);
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="street"]')).toBeNull();
  });

  it('shows address fields once delivery is selected', () => {
    cartService.addItem(margherita, [], 1);
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();

    const deliveryRadio = fixture.nativeElement.querySelector(
      '[data-testid="order-type-delivery"]',
    ) as HTMLInputElement;
    deliveryRadio.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="street"]')).not.toBeNull();
  });

  it('blocks submission when delivery address fields are missing', () => {
    cartService.addItem(margherita, [], 1);
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();
    fillRequiredContactFields(fixture);

    const deliveryRadio = fixture.nativeElement.querySelector(
      '[data-testid="order-type-delivery"]',
    ) as HTMLInputElement;
    deliveryRadio.click();
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    httpMock.expectNone('/api/orders');
    expect(fixture.nativeElement.textContent).toContain('Dieses Feld ist erforderlich.');
  });

  it('submits the correct payload for a pickup order and navigates to the confirmation', () => {
    cartService.addItem(margherita, [], 2);
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();
    fillRequiredContactFields(fixture);
    const navigateSpy = vi.spyOn(router, 'navigate');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/orders');
    expect(req.request.body).toEqual({
      orderType: 'PICKUP',
      customerData: {
        firstName: 'Mario',
        lastName: 'Rossi',
        phone: '0123456789',
        email: 'mario@example.com',
        street: null,
        houseNumber: null,
        postalCode: null,
        city: null,
      },
      items: [{ pizzaId: 1, quantity: 2, toppingIds: [] }],
    });

    const response: OrderResponse = {
      id: 42,
      orderNumber: 100042,
      publicToken: 'token-42',
      createdAt: new Date().toISOString(),
      status: 'NEW',
      orderType: 'PICKUP',
      paymentStatus: 'NOT_REQUIRED',
      customerData: {
        firstName: 'Mario',
        lastName: 'Rossi',
        phone: '0123456789',
        email: 'mario@example.com',
      },
      items: [],
      totalPrice: 15,
    };
    req.flush(response);

    expect(navigateSpy).toHaveBeenCalledWith(['/order-confirmation', 'token-42']);
    expect(cartService.items()).toEqual([]);
  });

  it('includes address fields in the payload for a delivery order', () => {
    cartService.addItem(margherita, [], 1);
    const fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();
    fillRequiredContactFields(fixture);

    const deliveryRadio = fixture.nativeElement.querySelector(
      '[data-testid="order-type-delivery"]',
    ) as HTMLInputElement;
    deliveryRadio.click();
    fixture.detectChanges();

    setInput(fixture.nativeElement, 'street', 'Hauptstraße');
    setInput(fixture.nativeElement, 'houseNumber', '12');
    setInput(fixture.nativeElement, 'postalCode', '12345');
    setInput(fixture.nativeElement, 'city', 'Berlin');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/orders');
    expect(req.request.body.orderType).toBe('DELIVERY');
    expect(req.request.body.customerData.street).toBe('Hauptstraße');
    expect(req.request.body.customerData.city).toBe('Berlin');
    req.flush({
      id: 43,
      orderNumber: 100043,
      publicToken: 'token-43',
      createdAt: new Date().toISOString(),
      status: 'NEW',
      orderType: 'DELIVERY',
      paymentStatus: 'NOT_REQUIRED',
      customerData: {
        firstName: 'Mario',
        lastName: 'Rossi',
        phone: '0123456789',
        email: 'mario@example.com',
      },
      items: [],
      totalPrice: 7.5,
    } satisfies OrderResponse);
  });
});
