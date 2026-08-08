export interface Topping {
  id: number;
  name: string;
  description: string | null;
  price: number;
}

export interface Pizza {
  id: number;
  name: string;
  description: string | null;
  price: number;
  imagePath: string | null;
  sortOrder: number;
  toppings: Topping[];
}
