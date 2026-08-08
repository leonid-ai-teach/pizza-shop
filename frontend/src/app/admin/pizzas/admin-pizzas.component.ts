import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AdminPizza, AdminTopping, PizzaWriteRequest } from '../admin.models';
import { ApiError } from '../../core/models/api-error.model';

@Component({
  selector: 'app-admin-pizzas',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './admin-pizzas.component.html',
  styleUrl: './admin-pizzas.component.css',
})
export class AdminPizzasComponent {
  private readonly adminApi = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly pizzas = signal<AdminPizza[]>([]);
  protected readonly toppings = signal<AdminTopping[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly selectedToppingIds = signal<number[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0.01)]],
    imagePath: [''],
    sortOrder: [0, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    this.loadPizzas();
    this.adminApi.getToppings().subscribe({
      next: (toppings) => this.toppings.set(toppings),
      error: () => this.error.set('Die Toppings konnten nicht geladen werden.'),
    });
  }

  protected isToppingSelected(toppingId: number): boolean {
    return this.selectedToppingIds().includes(toppingId);
  }

  protected toggleTopping(toppingId: number): void {
    this.selectedToppingIds.update((ids) =>
      ids.includes(toppingId) ? ids.filter((id) => id !== toppingId) : [...ids, toppingId],
    );
  }

  protected startCreate(): void {
    this.editingId.set(null);
    this.selectedToppingIds.set([]);
    this.form.reset({ name: '', description: '', price: 0, imagePath: '', sortOrder: 0 });
  }

  protected startEdit(pizza: AdminPizza): void {
    this.editingId.set(pizza.id);
    this.selectedToppingIds.set(pizza.toppings.map((t) => t.id));
    this.form.setValue({
      name: pizza.name,
      description: pizza.description ?? '',
      price: pizza.price,
      imagePath: pizza.imagePath ?? '',
      sortOrder: pizza.sortOrder,
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    const request: PizzaWriteRequest = {
      name: values.name,
      description: values.description || null,
      price: values.price,
      imagePath: values.imagePath || null,
      sortOrder: values.sortOrder,
      toppingIds: this.selectedToppingIds(),
    };

    const editingId = this.editingId();
    const call = editingId
      ? this.adminApi.updatePizza(editingId, request)
      : this.adminApi.createPizza(request);

    this.error.set(null);
    call.subscribe({
      next: () => {
        this.startCreate();
        this.loadPizzas();
      },
      error: (err: { error?: ApiError }) =>
        this.error.set(err.error?.message ?? 'Die Pizza konnte nicht gespeichert werden.'),
    });
  }

  protected toggleActive(pizza: AdminPizza): void {
    this.adminApi.setPizzaActive(pizza.id, !pizza.active).subscribe({
      next: () => this.loadPizzas(),
      error: () => this.error.set('Der Status konnte nicht geändert werden.'),
    });
  }

  private loadPizzas(): void {
    this.adminApi.getPizzas().subscribe({
      next: (pizzas) => this.pizzas.set(pizzas),
      error: () => this.error.set('Die Pizzen konnten nicht geladen werden.'),
    });
  }
}
