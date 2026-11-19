import { Component, EventEmitter, HostListener, inject, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../../../common/model/Product';
import { ProductComponent } from '../product/product.component';
import { CartService } from '../../../../cart/service/cart.service';

@Component({
  selector: 'app-shelf-bar',
  standalone: true,
  imports: [CommonModule, ProductComponent],
  templateUrl: './shelf-bar.component.html',
  styleUrl: './shelf-bar.component.css',
})
export class ShelfBarComponent {
  @Input() products: Product[] = [];
  cartService = inject(CartService)  
  recentlyAddedIndex = signal<number | null>(null);


  selected = signal<Product | null>(null);
  recentlyAdded = signal<string | null>(null);
  private clearTimer?: ReturnType<typeof setTimeout>;

  open(p: Product) {
    this.selected.set(p);
    document.body.style.overflow = 'hidden';
  }

  close() {
    this.selected.set(null);
    document.body.style.overflow = '';
  }

  addToCart(p: Product, index: number) {
    this.cartService.addToCart(p)

    this.recentlyAddedIndex.set(index);

    setTimeout(() => {
      if (this.recentlyAddedIndex() === index) {
        this.recentlyAddedIndex.set(null);
      }
    }, 1000);

  }

  @HostListener('document:keydown.escape')
  onEsc() {
    if (this.selected()) this.close();
  }
} 