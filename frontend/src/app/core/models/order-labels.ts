import { OrderStatus, OrderType } from './order.model';

/**
 * German display labels for the backend's English enum values. Code identifiers stay
 * English, German appears only as UI text (docs/Pizza_Shop_Master_Prompt.md §2).
 */
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  NEW: 'Neu',
  IN_PROGRESS: 'In Bearbeitung',
  DONE: 'Fertiggestellt',
  CANCELLED: 'Storniert',
};

export const ORDER_TYPE_LABELS: Record<OrderType, string> = {
  DELIVERY: 'Lieferung',
  PICKUP: 'Abholung',
};
