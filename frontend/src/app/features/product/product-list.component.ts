import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from './service/product.service';
import { Product } from './model/Product';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  host: {
    '(document:keydown.escape)': 'closeBook()',
  },
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);

  private readonly booksPerShelf = 6;
  private readonly palette = [
    '#8B2E2E', '#B8860B', '#2E5D3A', '#1B3A5C', '#5C2E5C',
    '#8B4513', '#2F4F4F', '#7B3F00', '#4A2E2E', '#3D5A2E',
  ];

  private allProducts = signal<Product[]>([]);
  products = signal<Product[]>([]);

  // Estado del libro abierto
  selectedBook = signal<Product | null>(null);

  // Paginación
  currentPage = signal(1);
  pageSize = signal(12);
  totalPages = computed(() =>
    Math.max(1, Math.ceil(this.allProducts().length / this.pageSize()))
  );
  rangeStart = computed(() =>
    this.allProducts().length === 0 ? 0 : (this.currentPage() - 1) * this.pageSize() + 1
  );
  rangeEnd = computed(() =>
    Math.min(this.currentPage() * this.pageSize(), this.allProducts().length)
  );
  pages = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i + 1));

  shelves = computed(() => {
    const list = this.products();
    const rows: Product[][] = [];
    for (let i = 0; i < list.length; i += this.booksPerShelf) {
      rows.push(list.slice(i, i + this.booksPerShelf));
    }
    return rows;
  });

  constructor() {
    // Bloquea el scroll del body mientras el libro está abierto
    effect(() => {
      document.body.style.overflow = this.selectedBook() ? 'hidden' : '';
    });
  }

  ngOnInit(): void {
    this.productService.getProducts().subscribe({
      next: (data) => {
        this.allProducts.set(JSON.parse(data) as Product[]);
        this.updatePage();
      },
      error: (error) => console.error('Error fetching products:', error),
    });
  }

  openBook(product: Product): void {
    this.selectedBook.set(product);
  }

  closeBook(): void {
    this.selectedBook.set(null);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;
    this.currentPage.set(page);
    this.updatePage();
  }
  nextPage(): void { this.goToPage(this.currentPage() + 1); }
  prevPage(): void { this.goToPage(this.currentPage() - 1); }

  private updatePage(): void {
    const start = (this.currentPage() - 1) * this.pageSize();
    this.products.set(this.allProducts().slice(start, start + this.pageSize()));
  }

  // ---- Helpers visuales ----
  getBookColor(product: Product): string {
    const hash = product.name.split('').reduce((a, c) => a + c.charCodeAt(0), 0);
    return this.palette[hash % this.palette.length];
  }
  getBookHeight(product: Product): number {
    const hash = product.name.split('').reduce((a, c) => a + c.charCodeAt(0) * 31, 7);
    return 190 + Math.abs(hash) % 60;
  }
  getBookWidth(product: Product): number {
    const hash = product.name.split('').reduce((a, c) => a + c.charCodeAt(0) * 17, 3);
    return 60 + Math.abs(hash) % 32;
  }
}