import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AdminApiService } from './admin-api.service';

/**
 * The server is the authority on whether the session is a signed-in admin, so the guard asks it
 * rather than trusting anything held client-side. A 401/403 sends the visitor to the login page.
 */
export const adminAuthGuard: CanActivateFn = () => {
  const adminApi = inject(AdminApiService);
  const router = inject(Router);

  return adminApi.me().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/admin/login']))),
  );
};
