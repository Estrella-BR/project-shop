
import { Injectable, computed, signal, PLATFORM_ID, inject  } from '@angular/core';
import { Product } from '../../common/model/Product';
import { CartProduct } from '../model/CartProduct';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly COOKIE_NAME = 'shopping_cart';
  private readonly COOKIE_DAYS = 30;
  private readonly CHUNK_SIZE = 3500; // encoded chars per cookie

  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private _list = signal<CartProduct[]>(this.loadCart());

  readonly list = this._list.asReadonly();


  readonly totalPrice = computed(() => {
  const sum = this._list().reduce((total, item) => total + item.totalPrice, 0);
  return Math.round(sum * 100) / 100; // stays a number, no 0.30000000000000004
});

  readonly totalQuantity = computed(() =>
    this._list().reduce((total, item) => total + item.quantity, 0)
  );

  private readCookie(name: string): string | null {
    const prefix = name + '=';
    for (const part of document.cookie.split('; ')) {
      if (part.startsWith(prefix)) {
        return decodeURIComponent(part.substring(prefix.length));
      }
    }
    return null;
  }

  private writeCookie(name: string, value: string): void {
    const maxAge = this.COOKIE_DAYS * 24 * 60 * 60;
    document.cookie =
      `${name}=${encodeURIComponent(value)}; max-age=${maxAge}; path=/; SameSite=Lax`;
  }

  private deleteCookie(name: string): void {
    document.cookie = `${name}=; max-age=0; path=/; SameSite=Lax`;
  }

  private loadCart(): CartProduct[] {
    if (!this.isBrowser) return [];

    try {
      // A cookie holds ~4KB, so the cart is split across cart_0, cart_1, ...
      const count = Number(this.readCookie(this.COOKIE_NAME + '_n') ?? 0);
      let savedCart = '';
      for (let i = 0; i < count; i++) {
        savedCart += this.readCookie(`${this.COOKIE_NAME}_${i}`) ?? '';
      }
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
      const json = JSON.stringify(this._list());
      const oldCount = Number(this.readCookie(this.COOKIE_NAME + '_n') ?? 0);

      // Chunk by encoded size so each cookie stays under the ~4KB limit.
      const chunks: string[] = [];
      let current = '';
      for (const ch of json) {
        if (encodeURIComponent(current + ch).length > this.CHUNK_SIZE) {
          chunks.push(current);
          current = '';
        }
        current += ch;
      }
      if (current) chunks.push(current);

      chunks.forEach((c, i) => this.writeCookie(`${this.COOKIE_NAME}_${i}`, c));
      for (let i = chunks.length; i < oldCount; i++) {
        this.deleteCookie(`${this.COOKIE_NAME}_${i}`);
      }
      this.writeCookie(this.COOKIE_NAME + '_n', String(chunks.length));
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
