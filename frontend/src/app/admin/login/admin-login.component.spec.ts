import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AdminLoginComponent } from './admin-login.component';

describe('AdminLoginComponent', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent],
      providers: [
        provideRouter([{ path: 'admin', redirectTo: '' }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  function render() {
    const fixture = TestBed.createComponent(AdminLoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  function type(el: HTMLElement, testId: string, value: string) {
    const input = el.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(el: HTMLElement) {
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  }

  it('posts the credentials and goes to the dashboard', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'login-email', 'chef@pizzashop.de');
    type(el, 'login-password', 'ein-gutes-passwort');
    submit(el);

    const req = httpMock.expectOne('/api/admin/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'chef@pizzashop.de',
      password: 'ein-gutes-passwort',
    });
    req.flush(null);

    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('reports a failed login and clears the password', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'login-email', 'chef@pizzashop.de');
    type(el, 'login-password', 'falsches-passwort');
    submit(el);

    httpMock.expectOne('/api/admin/login').flush(
      {
        timestamp: '2026-08-08T10:00:00Z',
        status: 401,
        error: 'INVALID_CREDENTIALS',
        message: 'Email or password is not correct.',
      },
      { status: 401, statusText: 'Unauthorized' },
    );
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="login-error"]')).not.toBeNull();
    expect(navigateSpy).not.toHaveBeenCalled();

    // The password field must not keep a wrong secret sitting on screen.
    const password = el.querySelector('[data-testid="login-password"]') as HTMLInputElement;
    expect(password.value).toBe('');
  });

  it('does not send anything when the form is empty', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    submit(el);
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/login');
    expect(el.textContent).toContain('gültige E-Mail-Adresse');
  });

  it('does not send an invalid email address', () => {
    const fixture = render();
    const el = fixture.nativeElement as HTMLElement;

    type(el, 'login-email', 'not-an-email');
    type(el, 'login-password', 'ein-gutes-passwort');
    submit(el);
    fixture.detectChanges();

    httpMock.expectNone('/api/admin/login');
    expect(el.textContent).toContain('gültige E-Mail-Adresse');
  });
});
