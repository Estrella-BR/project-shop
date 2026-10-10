import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/common/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'products',
    loadComponent: () => import('./features/product/product-list.component').then(m => m.ProductListComponent),
  },
  { path: '**', redirectTo: '' },
];
