import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../cart/cart.service';
import { OrderApiService } from '../core/services/order-api.service';
import { ApiError } from '../core/models/api-error.model';
import { CreateOrderRequest, OrderType } from '../core/models/order.model';

@Component({
  selector: 'app-checkout',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css',
})
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly orderApi = inject(OrderApiService);

  protected readonly cartItems = this.cartService.items;
  protected readonly cartTotal = this.cartService.total;
  protected readonly submitError = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    orderType: this.fb.nonNullable.control<OrderType>('PICKUP', Validators.required),
    firstName: this.fb.nonNullable.control('', Validators.required),
    lastName: this.fb.nonNullable.control('', Validators.required),
    phone: this.fb.nonNullable.control('', Validators.required),
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    street: this.fb.control(''),
    houseNumber: this.fb.control(''),
    postalCode: this.fb.control(''),
    city: this.fb.control(''),
  });

  constructor() {
    if (this.cartService.items().length === 0) {
      this.router.navigate(['/cart']);
    }

    this.form.controls.orderType.valueChanges.subscribe((orderType) => {
      const addressControls = [
        this.form.controls.street,
        this.form.controls.houseNumber,
        this.form.controls.postalCode,
        this.form.controls.city,
      ];
      for (const control of addressControls) {
        if (orderType === 'DELIVERY') {
          control.setValidators(Validators.required);
        } else {
          control.clearValidators();
        }
        control.updateValueAndValidity();
      }
    });
  }

  protected get isDelivery(): boolean {
    return this.form.controls.orderType.value === 'DELIVERY';
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();
    const request: CreateOrderRequest = {
      orderType: values.orderType,
      customerData: {
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        email: values.email,
        street: values.orderType === 'DELIVERY' ? values.street : null,
        houseNumber: values.orderType === 'DELIVERY' ? values.houseNumber : null,
        postalCode: values.orderType === 'DELIVERY' ? values.postalCode : null,
        city: values.orderType === 'DELIVERY' ? values.city : null,
      },
      items: this.cartService.items().map((item) => ({
        pizzaId: item.pizza.id,
        quantity: item.quantity,
        toppingIds: item.toppings.map((t) => t.id),
      })),
    };

    this.submitting.set(true);
    this.submitError.set(null);
    this.orderApi.createOrder(request).subscribe({
      next: (order) => {
        this.cartService.clear();
        this.router.navigate(['/order-confirmation', order.publicToken]);
      },
      error: (err: { error?: ApiError }) => {
        this.submitting.set(false);
        this.submitError.set(
          err.error?.message ?? 'Es ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.',
        );
      },
    });
  }
}
