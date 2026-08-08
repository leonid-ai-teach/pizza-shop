import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { CartService } from './cart/cart.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  protected readonly cartItemCount = this.cartService.itemCount;
  /**
   * The admin area brings its own navigation; the shop header would just be noise there.
   * Seeded from the actual URL rather than router.url, which is still "/" at construction
   * time — otherwise the shop header flashes over /admin for the whole guard round-trip.
   */
  protected readonly showShopHeader = signal(!location.pathname.startsWith('/admin'));

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.showShopHeader.set(!event.urlAfterRedirects.startsWith('/admin')));
  }
}
