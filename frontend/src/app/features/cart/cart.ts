import { Component, inject, signal, OnDestroy } from '@angular/core';
import { CartService } from './service/cart.service';
import { CartItemComponent } from './components/cart-item/cart-item';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CartItemComponent],
  templateUrl: './cart.html',
  styleUrl: './cart.css'
})
export class CartComponent implements OnDestroy {

  private cartService = inject(CartService);

  list = this.cartService.list;
  totalPrice = this.cartService.totalPrice;
  totalQuantity = this.cartService.totalQuantity;

  /** Two-step confirmation state for the "Clear all" button */
  confirmingClear = signal(false);
  private confirmTimeout?: ReturnType<typeof setTimeout>;

  requestClear(): void {
    if (!this.confirmingClear()) {
      this.confirmingClear.set(true);
      this.confirmTimeout = setTimeout(() => this.confirmingClear.set(false), 3000);
      return;
    }

    clearTimeout(this.confirmTimeout);
    this.confirmingClear.set(false);
    this.clearCart();
  }

  increaseQuantity(product: any): void {
    this.cartService.increaseQuantity(product);
  }

  decreaseQuantity(product: any): void {
    this.cartService.decreaseQuantity(product);
  }

  removeFromCart(product: any): void {
    this.cartService.removeFromCart(product);
  }

  clearCart(): void {
    this.cartService.clearCart();
  }

  checkout(): void {
    console.log('Checkout:', this.list());
  }

  ngOnDestroy(): void {
    clearTimeout(this.confirmTimeout);
  }
}