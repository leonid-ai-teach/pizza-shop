import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminAdminsComponent } from './admin-admins.component';
import { AdminAccessEntry } from '../admin.models';

const existingAdmin: AdminAccessEntry = {
  email: 'chef@pizzashop.de',
  approvedAt: '2026-08-01T09:00:00Z',
  approvedBy: 'bootstrap',
};

describe('AdminAdminsComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminAdminsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function render() {
    const fixture = TestBed.createComponent(AdminAdminsComponent);
    httpMock.expectOne('/api/admin/admins').flush([existingAdmin]);
    fixture.detectChanges();
    return fixture;
  }

  it('lists the current allowlist with who approved each entry', () => {
    const fixture = render();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('chef@pizzashop.de');
    expect(text).toContain('bootstrap');
  });

  it('posts an invitation and refreshes the list', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    const input = el.querySelector('[data-testid="invite-email"]') as HTMLInputElement;
    input.value = 'newhire@pizzashop.de';
    input.dispatchEvent(new Event('input'));

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/admin/admins');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'newhire@pizzashop.de' });
    req.flush({ ...existingAdmin, email: 'newhire@pizzashop.de' });

    httpMock.expectOne('/api/admin/admins').flush([existingAdmin]);
  });

  it('does not post an invalid email address', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    const input = el.querySelector('[data-testid="invite-email"]') as HTMLInputElement;
    input.value = 'not-an-email';
    input.dispatchEvent(new Event('input'));

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/admins');
    expect(el.textContent).toContain('gültige E-Mail-Adresse');
  });

  it('shows the server message when an invite is rejected', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    const input = el.querySelector('[data-testid="invite-email"]') as HTMLInputElement;
    input.value = 'chef@pizzashop.de';
    input.dispatchEvent(new Event('input'));
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    httpMock.expectOne('/api/admin/admins').flush(
      {
        timestamp: '2026-08-08T10:00:00Z',
        status: 400,
        error: 'VALIDATION_ERROR',
        message: 'This email is already an admin: chef@pizzashop.de',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="admins-error"]')?.textContent).toContain(
      'already an admin',
    );
  });
});
