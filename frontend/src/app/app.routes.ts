import { Routes } from '@angular/router';
import { MenuComponent } from './shop/menu/menu.component';
import { PizzaDetailComponent } from './shop/pizza-detail/pizza-detail.component';
import { CartPageComponent } from './cart/cart-page/cart-page.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderConfirmationComponent } from './checkout/order-confirmation/order-confirmation.component';

export const routes: Routes = [
  { path: '', component: MenuComponent },
  { path: 'pizza/:id', component: PizzaDetailComponent },
  { path: 'cart', component: CartPageComponent },
  { path: 'checkout', component: CheckoutComponent },
  { path: 'order-confirmation/:id', component: OrderConfirmationComponent },
];
