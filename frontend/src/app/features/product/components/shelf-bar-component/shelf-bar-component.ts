import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../model/Product'; // Ajusta la ruta

@Component({
  selector: 'app-shelf-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shelf-bar-component.html',
  styleUrl: './shelf-bar-component.css',
  
})
export class ShelfBarComponent {
  @Input() products: Product[] = [];
}