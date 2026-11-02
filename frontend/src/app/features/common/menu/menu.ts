import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './menu.html',
  styleUrl: './menu.css',
})
export class MenuComponent {

  options = [
    { label: 'Home', path: '/' },
    { label: 'Libros', path: '/books' },
    { label: 'Productos', path: '/products' },
  ];
}
