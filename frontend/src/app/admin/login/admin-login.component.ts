import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-admin-login',
  imports: [],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.css',
})
export class AdminLoginComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly loginFailed = signal(
    this.route.snapshot.queryParamMap.has('error'),
  );

  protected startGoogleLogin(): void {
    // A full page navigation, not an XHR: the OAuth2 flow needs to leave the app and come back.
    window.location.href = '/oauth2/authorization/google';
  }
}
