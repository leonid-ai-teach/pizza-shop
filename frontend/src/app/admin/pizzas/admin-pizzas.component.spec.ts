import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { AdminPizzasComponent } from './admin-pizzas.component';
import { AdminPizza, AdminTopping } from '../admin.models';

registerLocaleData(localeDe);

const cheese: AdminTopping = {
  id: 1,
  name: 'Extra Käse',
  description: null,
  price: 1.0,
  active: true,
};
const pineapple: AdminTopping = {
  id: 2,
  name: 'Ananas',
  description: null,
  price: 1.2,
  active: false,
};

const margherita: AdminPizza = {
  id: 10,
  name: 'Margherita',
  description: 'Klassisch',
  price: 7.5,
  imagePath: null,
  active: true,
  sortOrder: 10,
  toppings: [cheese],
};

const retired: AdminPizza = { ...margherita, id: 11, name: 'Retired', active: false };

describe('AdminPizzasComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPizzasComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function render(pizzas: AdminPizza[] = [margherita, retired]) {
    const fixture = TestBed.createComponent(AdminPizzasComponent);
    httpMock.expectOne('/api/admin/pizzas').flush(pizzas);
    httpMock.expectOne('/api/admin/toppings').flush([cheese, pineapple]);
    fixture.detectChanges();
    return fixture;
  }

  it('lists inactive pizzas too, marked as inactive', () => {
    const fixture = render();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Margherita');
    expect(text).toContain('Retired');
    expect(text).toContain('Inaktiv');
  });

  it('creates a pizza with the selected topping assignment', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    setInput(el, 'pizza-name', 'Tonno');
    setInput(el, 'pizza-price', '9.5');
    setInput(el, 'pizza-sort-order', '30');
    (el.querySelector('[data-testid="assign-topping-1"]') as HTMLInputElement).click();

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/admin/pizzas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      name: 'Tonno',
      description: null,
      price: 9.5,
      imagePath: null,
      sortOrder: 30,
      toppingIds: [1],
    });
    req.flush(margherita);
    httpMock.expectOne('/api/admin/pizzas').flush([margherita]);
  });

  it('pre-fills the form when editing and PUTs to the pizza id', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('[data-testid="edit-pizza-10"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect((el.querySelector('[data-testid="pizza-name"]') as HTMLInputElement).value).toBe(
      'Margherita',
    );
    expect((el.querySelector('[data-testid="assign-topping-1"]') as HTMLInputElement).checked).toBe(
      true,
    );

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/admin/pizzas/10');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.toppingIds).toEqual([1]);
    req.flush(margherita);
    httpMock.expectOne('/api/admin/pizzas').flush([margherita]);
  });

  it('deactivates a pizza via the active endpoint rather than deleting it', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('[data-testid="toggle-pizza-10"]') as HTMLButtonElement).click();

    const req = httpMock.expectOne('/api/admin/pizzas/10/active');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
    req.flush({ ...margherita, active: false });
    httpMock.expectOne('/api/admin/pizzas').flush([{ ...margherita, active: false }]);
  });

  it('does not submit when required fields are missing', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/pizzas');
    expect(el.textContent).toContain('Dieses Feld ist erforderlich.');
  });

  it('surfaces the server error message when saving fails', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    setInput(el, 'pizza-name', 'Tonno');
    setInput(el, 'pizza-price', '9.5');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    httpMock.expectOne('/api/admin/pizzas').flush(
      {
        timestamp: '2026-08-08T10:00:00Z',
        status: 400,
        error: 'VALIDATION_ERROR',
        message: 'One or more toppings do not exist.',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="pizzas-error"]')?.textContent).toContain(
      'One or more toppings do not exist.',
    );
  });
});

function setInput(root: HTMLElement, testId: string, value: string) {
  const input = root.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}
