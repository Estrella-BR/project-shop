import { Component, HostListener, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../../model/Product'; // Ajusta la ruta

@Component({
  selector: 'app-shelf-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shelf-bar.component.html',
  styleUrl: './shelf-bar.component.css',
  
})
export class ShelfBarComponent {
  @Input() products: Product[] = [];

   selected = signal<Product | null>(null);

  open(p: Product) {
    this.selected.set(p);
    document.body.style.overflow = 'hidden'; // bloquea scroll de fondo
  }

  close() {
    this.selected.set(null);
    document.body.style.overflow = '';
  }

  @HostListener('document:keydown.escape')
  onEsc() {
    if (this.selected()) this.close();
  }
}