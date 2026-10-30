import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/common/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'books',
    loadComponent: () => import('./features/book/book-list.component').then(m => m.BookListComponent),
  },
  { path: '**', redirectTo: '' },
];
