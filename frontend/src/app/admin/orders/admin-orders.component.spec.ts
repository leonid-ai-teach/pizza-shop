import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { AdminOrdersComponent } from './admin-orders.component';
import { OrderResponse, OrderStatus } from '../../core/models/order.model';

registerLocaleData(localeDe);

function order(id: number, status: OrderStatus): OrderResponse {
  return {
    id,
    orderNumber: 100000 + id,
    publicToken: `token-${id}`,
    createdAt: '2026-08-08T10:00:00Z',
    status,
    orderType: 'PICKUP',
    paymentStatus: 'NOT_REQUIRED',
    customerData: {
      firstName: 'Mario',
      lastName: 'Rossi',
      phone: '0123',
      email: 'mario@example.com',
    },
    items: [
      {
        pizzaId: 1,
        pizzaName: 'Margherita',
        quantity: 1,
        basePrice: 7.5,
        toppings: [],
        itemTotalPrice: 7.5,
      },
    ],
    totalPrice: 7.5,
  };
}

describe('AdminOrdersComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminOrdersComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function render(orders: OrderResponse[]) {
    const fixture = TestBed.createComponent(AdminOrdersComponent);
    httpMock.expectOne('/api/admin/orders').flush(orders);
    fixture.detectChanges();
    return fixture;
  }

  it('lists orders with German status labels', () => {
    const fixture = render([order(1, 'NEW'), order(2, 'IN_PROGRESS')]);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('#100001');
    expect(text).toContain('Neu');
    expect(text).toContain('In Bearbeitung');
  });

  it('refetches with a status parameter when the filter changes', () => {
    const fixture = render([order(1, 'NEW')]);

    const select = fixture.nativeElement.querySelector(
      '[data-testid="status-filter"]',
    ) as HTMLSelectElement;
    select.value = 'DONE';
    select.dispatchEvent(new Event('change'));

    const req = httpMock.expectOne((r) => r.url === '/api/admin/orders');
    expect(req.request.params.get('status')).toBe('DONE');
    req.flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="no-orders"]')).not.toBeNull();
  });

  it('offers only the legal next statuses for an open order', () => {
    const fixture = render([order(1, 'NEW')]);

    (fixture.nativeElement.querySelector('[data-testid="view-order-1"]') as HTMLButtonElement).click();
    httpMock.expectOne('/api/admin/orders/1').flush(order(1, 'NEW'));
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="set-status-IN_PROGRESS"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="set-status-CANCELLED"]')).not.toBeNull();
    // NEW -> DONE is not a legal transition, so the UI must not offer it
    expect(el.querySelector('[data-testid="set-status-DONE"]')).toBeNull();
  });

  it('offers no status actions for a terminal order', () => {
    const fixture = render([order(3, 'DONE')]);

    (fixture.nativeElement.querySelector('[data-testid="view-order-3"]') as HTMLButtonElement).click();
    httpMock.expectOne('/api/admin/orders/3').flush(order(3, 'DONE'));
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="no-transitions"]')).not.toBeNull();
    expect(el.querySelectorAll('[data-testid^="set-status-"]').length).toBe(0);
  });

  it('patches the status and reloads the list when an action is used', () => {
    const fixture = render([order(1, 'NEW')]);

    (fixture.nativeElement.querySelector('[data-testid="view-order-1"]') as HTMLButtonElement).click();
    httpMock.expectOne('/api/admin/orders/1').flush(order(1, 'NEW'));
    fixture.detectChanges();

    (
      fixture.nativeElement.querySelector(
        '[data-testid="set-status-IN_PROGRESS"]',
      ) as HTMLButtonElement
    ).click();

    const patchReq = httpMock.expectOne('/api/admin/orders/1/status');
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({ status: 'IN_PROGRESS' });
    patchReq.flush(order(1, 'IN_PROGRESS'));

    httpMock.expectOne('/api/admin/orders').flush([order(1, 'IN_PROGRESS')]);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('In Bearbeitung');
  });

  it('shows the delivery address only for delivery orders', () => {
    const deliveryOrder: OrderResponse = {
      ...order(4, 'NEW'),
      orderType: 'DELIVERY',
      customerData: {
        firstName: 'Anna',
        lastName: 'Schmidt',
        phone: '030',
        email: 'anna@example.de',
        street: 'Hauptstraße',
        houseNumber: '12',
        postalCode: '12345',
        city: 'Berlin',
      },
    };
    const fixture = render([deliveryOrder]);

    (fixture.nativeElement.querySelector('[data-testid="view-order-4"]') as HTMLButtonElement).click();
    httpMock.expectOne('/api/admin/orders/4').flush(deliveryOrder);
    fixture.detectChanges();

    const address = fixture.nativeElement.querySelector('[data-testid="delivery-address"]');
    expect(address?.textContent).toContain('Hauptstraße');
    expect(address?.textContent).toContain('Berlin');
  });
});
