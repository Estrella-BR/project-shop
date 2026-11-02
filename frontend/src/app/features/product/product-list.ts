import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from './service/product.service';
import { Product } from './model/Product';
import { ProductComponent } from './components/product-component';
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ProductComponent],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  host: {
  },
})
export class ProductListComponent implements OnInit {

    productService = inject(ProductService);
    allProducts = signal<Product[]>([]);
    products = signal<Product[]>([]);

    
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

  
    ngOnInit(): void {
        this.productService.getProducts().subscribe({
            next: (data) => {
            this.allProducts.set(JSON.parse(data) as Product[]);
            this.updatePage();
            },
            error: (error) => console.error('Error fetching products:', error),
        });
    }

    
  private updatePage(): void {
    const start = (this.currentPage() - 1) * this.pageSize();
    this.products.set(this.allProducts().slice(start, start + this.pageSize()));
  }

    goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;
    this.currentPage.set(page);
    this.updatePage();
  }
  nextPage(): void { this.goToPage(this.currentPage() + 1); }
  prevPage(): void { this.goToPage(this.currentPage() - 1); }

  
}