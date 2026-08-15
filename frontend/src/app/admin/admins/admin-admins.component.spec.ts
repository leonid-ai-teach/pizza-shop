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

  function type(el: HTMLElement, testId: string, value: string) {
    const input = el.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  /** The invite form is the first of the two forms on the page. */
  function submitInvite(el: HTMLElement) {
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  }

  it('lists the current allowlist with who approved each entry', () => {
    const fixture = render();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('chef@pizzashop.de');
    expect(text).toContain('bootstrap');
  });

  it('posts an invitation with its initial password and refreshes the list', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'invite-email', 'newhire@pizzashop.de');
    type(el, 'invite-password', 'ein-gutes-passwort');
    submitInvite(el);

    const req = httpMock.expectOne('/api/admin/admins');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'newhire@pizzashop.de',
      password: 'ein-gutes-passwort',
    });
    req.flush({ ...existingAdmin, email: 'newhire@pizzashop.de' });

    httpMock.expectOne('/api/admin/admins').flush([existingAdmin]);
  });

  it('does not post an invalid email address', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'invite-email', 'not-an-email');
    type(el, 'invite-password', 'ein-gutes-passwort');
    submitInvite(el);
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/admins');
    expect(el.textContent).toContain('gültige E-Mail-Adresse');
  });

  it('does not post a password shorter than the backend accepts', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'invite-email', 'newhire@pizzashop.de');
    type(el, 'invite-password', 'kurz');
    submitInvite(el);
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/admins');
    expect(el.textContent).toContain('Mindestens 10 Zeichen');
  });

  it('changes the own password and confirms it', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'current-password', 'altes-passwort');
    type(el, 'new-password', 'ein-neues-passwort');
    (el.querySelectorAll('form')[1] as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = httpMock.expectOne('/api/admin/me/password');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      currentPassword: 'altes-passwort',
      newPassword: 'ein-neues-passwort',
    });
    req.flush(null);
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="password-changed"]')).not.toBeNull();
  });

  it('shows the server message when the current password is wrong', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'current-password', 'falsches-passwort');
    type(el, 'new-password', 'ein-neues-passwort');
    (el.querySelectorAll('form')[1] as HTMLFormElement).dispatchEvent(new Event('submit'));

    httpMock.expectOne('/api/admin/me/password').flush(
      {
        timestamp: '2026-08-08T10:00:00Z',
        status: 401,
        error: 'INVALID_CREDENTIALS',
        message: 'The current password is not correct.',
      },
      { status: 401, statusText: 'Unauthorized' },
    );
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="password-error"]')?.textContent).toContain(
      'not correct',
    );
  });

  it('shows the server message when an invite is rejected', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'invite-email', 'chef@pizzashop.de');
    type(el, 'invite-password', 'ein-gutes-passwort');
    submitInvite(el);

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
