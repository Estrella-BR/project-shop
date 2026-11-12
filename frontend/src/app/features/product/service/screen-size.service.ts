import { computed, inject, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ScreenSizeService {
  private _screenSize = signal({ width: 0, height: 0 });
  readonly screenSize = this._screenSize.asReadonly();

  // Constantes que reflejan tu CSS
  private readonly SHELF_PADDING_X = 20;   // 10px * 2
  private readonly TAG_WIDTH_REM   = 15;
  private readonly ROOT_FONT_PX    = 16;   // asume 1rem = 16px

  // Cuántos price-tags caben en una fila
  readonly tagsPerRow = computed(() => {
    const { width } = this._screenSize();
    if (width === 0) return 0; // SSR
    const available = width - this.SHELF_PADDING_X;
    const tagWidth  = this.TAG_WIDTH_REM * this.ROOT_FONT_PX; // 240
    return Math.max(1, Math.floor(available / tagWidth) - 2);
  });

  constructor() {
    if (typeof window !== 'undefined') {
      this.updateSize();
      window.addEventListener('resize', () => this.updateSize());
    }
  }

  private updateSize() {
    this._screenSize.set({
      width:  window.innerWidth,
      height: window.innerHeight,
    });
  }
}