import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/common/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'books',
    loadComponent: () => import('./features/book/book-list').then(m => m.BookListComponent),
  },
  
  {
    path: 'products',
    loadComponent: () => import('./features/product/product-list').then(m => m.ProductListComponent),
  },
  { path: '**', redirectTo: '' },
];
