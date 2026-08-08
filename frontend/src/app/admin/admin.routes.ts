import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin-shell.component';
import { adminAuthGuard } from './admin-auth.guard';
import { AdminLoginComponent } from './login/admin-login.component';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';
import { AdminOrdersComponent } from './orders/admin-orders.component';
import { AdminPizzasComponent } from './pizzas/admin-pizzas.component';
import { AdminToppingsComponent } from './toppings/admin-toppings.component';
import { AdminAdminsComponent } from './admins/admin-admins.component';

export const adminRoutes: Routes = [
  { path: 'login', component: AdminLoginComponent },
  {
    path: '',
    component: AdminShellComponent,
    canActivate: [adminAuthGuard],
    children: [
      { path: '', component: AdminDashboardComponent },
      { path: 'orders', component: AdminOrdersComponent },
      { path: 'pizzas', component: AdminPizzasComponent },
      { path: 'toppings', component: AdminToppingsComponent },
      { path: 'admins', component: AdminAdminsComponent },
    ],
  },
];
