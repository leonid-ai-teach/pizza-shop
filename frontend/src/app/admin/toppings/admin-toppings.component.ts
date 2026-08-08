import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AdminTopping, ToppingWriteRequest } from '../admin.models';
import { ApiError } from '../../core/models/api-error.model';

@Component({
  selector: 'app-admin-toppings',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './admin-toppings.component.html',
  styleUrl: './admin-toppings.component.css',
})
export class AdminToppingsComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly toppings = signal<AdminTopping[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    this.loadToppings();
  }

  protected startCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '', price: 0 });
  }

  protected startEdit(topping: AdminTopping): void {
    this.editingId.set(topping.id);
    this.form.setValue({
      name: topping.name,
      description: topping.description ?? '',
      price: topping.price,
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    const request: ToppingWriteRequest = {
      name: values.name,
      description: values.description || null,
      price: values.price,
    };

    const editingId = this.editingId();
    const call = editingId
      ? this.adminApi.updateTopping(editingId, request)
      : this.adminApi.createTopping(request);

    this.error.set(null);
    call.subscribe({
      next: () => {
        this.startCreate();
        this.loadToppings();
      },
      error: (err: { error?: ApiError }) =>
        this.error.set(err.error?.message ?? 'Das Topping konnte nicht gespeichert werden.'),
    });
  }

  protected toggleActive(topping: AdminTopping): void {
    this.adminApi.setToppingActive(topping.id, !topping.active).subscribe({
      next: () => this.loadToppings(),
      error: () => this.error.set('Der Status konnte nicht geändert werden.'),
    });
  }

  private loadToppings(): void {
    this.adminApi.getToppings().subscribe({
      next: (toppings) => this.toppings.set(toppings),
      error: () => this.error.set('Die Toppings konnten nicht geladen werden.'),
    });
  }
}
