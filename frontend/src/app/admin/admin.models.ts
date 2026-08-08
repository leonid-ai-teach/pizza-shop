import { OrderStatus } from '../core/models/order.model';

export interface CurrentAdmin {
  email: string;
  name: string | null;
}

export interface AdminAccessEntry {
  email: string;
  approvedAt: string;
  approvedBy: string;
}

export interface OrderSummary {
  newCount: number;
  inProgressCount: number;
  doneCount: number;
}

export interface AdminTopping {
  id: number;
  name: string;
  description: string | null;
  price: number;
  active: boolean;
}

export interface AdminPizza {
  id: number;
  name: string;
  description: string | null;
  price: number;
  imagePath: string | null;
  active: boolean;
  sortOrder: number;
  toppings: AdminTopping[];
}

export interface PizzaWriteRequest {
  name: string;
  description: string | null;
  price: number;
  imagePath: string | null;
  sortOrder: number;
  toppingIds: number[];
}

export interface ToppingWriteRequest {
  name: string;
  description: string | null;
  price: number;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
}
