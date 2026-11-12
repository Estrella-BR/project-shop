import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../../model/Product'; 
import { ProductComponent } from '../product/product.component';
import { ShelfBarComponent } from '../shelf-bar/shelf-bar.component';
@Component({
  selector: 'app-shelf',
  standalone: true,
  imports: [CommonModule, ProductComponent, ShelfBarComponent],
  templateUrl: './shelf.component.html',
  styleUrl: './shelf.component.css',
})
export class ShelfComponent {
  @Input() products: Product[] = [];
}