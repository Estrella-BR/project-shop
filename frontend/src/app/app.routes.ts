import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/common/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'products',
    loadComponent: () => import('./features/product/product-list/product-list').then(m => m.ProductListComponent),
  },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat').then(m => m.ChatComponent),
  },
  {
    path: 'cart',
    loadComponent: () => import('./features/cart/cart').then(m => m.CartComponent),
  },
  { path: '**', redirectTo: '' },
];
