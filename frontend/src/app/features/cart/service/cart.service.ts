
import { Injectable, computed, signal, PLATFORM_ID, inject  } from '@angular/core';
import { Product } from '../../common/model/Product';
import { CartProduct } from '../model/CartProduct';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly STORAGE_KEY = 'shopping-cart';

  private _list = signal<CartProduct[]>(this.loadCart());

  readonly list = this._list.asReadonly();  
  private readonly platformId = inject(PLATFORM_ID);

  private readonly isBrowser = isPlatformBrowser(this.platformId);


  readonly totalPrice = computed(() => {
  const sum = this._list().reduce((total, item) => total + item.totalPrice, 0);
  return Math.round(sum * 100) / 100; // stays a number, no 0.30000000000000004
});

  readonly totalQuantity = computed(() =>
    this._list().reduce((total, item) => total + item.quantity, 0)
  );

  private loadCart(): CartProduct[] {
    if (!this.isBrowser) return [];

    try {
      const savedCart = localStorage.getItem(this.STORAGE_KEY);
      if (!savedCart) return [];

      const products: { product: Product; quantity: number }[] =
        JSON.parse(savedCart);

      return products.map(item => new CartProduct(item.product, item.quantity));
    } catch (error) {
      console.error('Error loading shopping cart:', error);
      return [];
    }
  }

  private saveCart(): void {
    if (!this.isBrowser) return;

    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this._list()));
    } catch (error) {
      console.error('Error saving shopping cart:', error);
    }
  }
  
  addToCart(product: Product): void {
    const index = this._list().findIndex(
      item => item.product.name === product.name
        && item.product.seller === product.seller
    );

    if (index === -1) {
      this._list.update(items => [
        ...items,
        new CartProduct(product, 1)
      ]);
    } else {
      this._list.update(items =>
        items.map((item, i) =>
          i === index
            ? new CartProduct(item.product, item.quantity + 1)
            : item
        )
      );
    }

    this.saveCart();
  }

  increaseQuantity(product: Product): void {
    this._list.update(items =>
      items.map(item =>
        this.isSameProduct(item.product, product)
          ? new CartProduct(item.product, item.quantity + 1)
          : item
      )
    );

    this.saveCart();
  }

  decreaseQuantity(product: Product): void {
    this._list.update(items =>
      items.map(item =>
        this.isSameProduct(item.product, product) && item.quantity > 1
          ? new CartProduct(item.product, item.quantity - 1)
          : item
      )
    );

    this.saveCart();
  }

  removeFromCart(product: Product): void {
    this._list.update(items =>
      items.filter(item => !this.isSameProduct(item.product, product))
    );

    this.saveCart();
  }

  clearCart(): void {
    this._list.set([]);
    this.saveCart();
  }

  private isSameProduct(a: Product, b: Product): boolean {
    return a.name === b.name && a.seller === b.seller;
  }
}
