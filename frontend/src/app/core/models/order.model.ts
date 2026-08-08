export type OrderType = 'DELIVERY' | 'PICKUP';
export type OrderStatus = 'NEW' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type PaymentStatus = 'NOT_REQUIRED';

export interface CustomerData {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  street?: string | null;
  houseNumber?: string | null;
  postalCode?: string | null;
  city?: string | null;
}

export interface OrderItemRequest {
  pizzaId: number;
  quantity: number;
  toppingIds: number[];
}

export interface CreateOrderRequest {
  orderType: OrderType;
  customerData: CustomerData;
  items: OrderItemRequest[];
}

export interface OrderItemToppingResponse {
  toppingId: number;
  name: string;
  price: number;
}

export interface OrderItemResponse {
  pizzaId: number;
  pizzaName: string;
  quantity: number;
  basePrice: number;
  toppings: OrderItemToppingResponse[];
  itemTotalPrice: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: number;
  createdAt: string;
  status: OrderStatus;
  orderType: OrderType;
  paymentStatus: PaymentStatus;
  customerData: CustomerData;
  items: OrderItemResponse[];
  totalPrice: number;
}
