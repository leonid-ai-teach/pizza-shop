import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../cart.service';
import { lineTotal, unitPrice } from '../cart-item.model';

@Component({
  selector: 'app-cart-page',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.css',
})
export class CartPageComponent {
  private readonly router = inject(Router);
  protected readonly cartService = inject(CartService);

  protected readonly unitPrice = unitPrice;
  protected readonly lineTotal = lineTotal;

  protected increaseQuantity(itemId: string, currentQuantity: number): void {
    this.cartService.updateQuantity(itemId, currentQuantity + 1);
  }

  protected decreaseQuantity(itemId: string, currentQuantity: number): void {
    this.cartService.updateQuantity(itemId, currentQuantity - 1);
  }

  protected removeItem(itemId: string): void {
    this.cartService.removeItem(itemId);
  }

  protected goToCheckout(): void {
    this.router.navigate(['/checkout']);
  }
}
