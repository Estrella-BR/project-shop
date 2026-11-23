import { Component, input, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ChatMessage } from '../../model/ChatMessage';
import { CartService } from '../../../cart/service/cart.service';
import { Product } from '../../../common/model/Product';
import { ChatProductCardComponent } from '../product-card/product-card';

@Component({
  imports: [DatePipe, ChatProductCardComponent],
  selector: 'app-chat-message',
  styleUrl: './chat-message.css',
  templateUrl: './chat-message.html',
})
export class ChatMessageComponent {

  message = input.required<ChatMessage>();

  private cartService = inject(CartService);

  addedProducts = signal<string[]>([]);

  addToCart(product: Product): void {
    this.cartService.addToCart(product);

    const key = this.getProductKey(product);

    this.addedProducts.update(products =>
      products.includes(key) ? products : [...products, key]
    );
  }

  isAdded(product: Product): boolean {
    return this.addedProducts().includes(this.getProductKey(product));
  }

  private getProductKey(product: Product): string {
    return `${product.name}-${product.seller}`;
  }
}