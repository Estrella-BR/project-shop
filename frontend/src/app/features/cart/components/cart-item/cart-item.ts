import { Component, input, output, signal } from '@angular/core';
import { CartProduct } from '../../model/CartProduct';

@Component({
  selector: 'app-cart-item',
  standalone: true,
  templateUrl: './cart-item.html',
  styleUrl: './cart-item.css'
})
export class CartItemComponent {

  item = input.required<CartProduct>();

  increase = output<void>();
  decrease = output<void>();
  remove = output<void>();
  purchased = signal(false);

  togglePurchased(): void {
    this.purchased.update(value => !value);
  }
}