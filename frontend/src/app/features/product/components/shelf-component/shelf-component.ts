import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../model/Product'; // Ajusta la ruta
import { ProductComponent } from '../product-component/product-component'; // Ajusta la ruta
import { ShelfBarComponent } from '../shelf-bar-component/shelf-bar-component';

@Component({
  selector: 'app-shelf',
  standalone: true,
  imports: [CommonModule, ProductComponent, ShelfBarComponent],
  templateUrl: './shelf-component.html',
  styleUrl: './shelf-component.css',
})
export class ShelfComponent {
  @Input() products: Product[] = [];
}