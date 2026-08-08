import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuardResult, Router, provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { adminAuthGuard } from './admin-auth.guard';

function runGuard(): Promise<GuardResult> {
  const result = TestBed.runInInjectionContext(() => adminAuthGuard(null!, null!));
  return firstValueFrom(isObservable(result) ? result : of(result as GuardResult));
}

describe('adminAuthGuard', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('allows access when the server confirms an admin session', async () => {
    const resultPromise = runGuard();

    httpMock.expectOne('/api/admin/me').flush({ email: 'chef@pizzashop.de', name: 'Chef' });

    expect(await resultPromise).toBe(true);
  });

  it('redirects to the login page when the session is not authenticated', async () => {
    const router = TestBed.inject(Router);
    const resultPromise = runGuard();

    httpMock.expectOne('/api/admin/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    const result = await resultPromise;
    expect(result).toEqual(router.createUrlTree(['/admin/login']));
  });

  it('redirects to the login page when the account is not allowlisted', async () => {
    const router = TestBed.inject(Router);
    const resultPromise = runGuard();

    httpMock.expectOne('/api/admin/me').flush(null, { status: 403, statusText: 'Forbidden' });

    const result = await resultPromise;
    expect(result).toEqual(router.createUrlTree(['/admin/login']));
  });
});
