import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from './service/product.service';
import { Product } from './model/Product';
import { ProductComponent } from './components/product-component';
import { PaginatedResponse } from '../common/model/PaginatedResponse';
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
    products = signal<Product[]>([]);

  totalElements = 0;
    
  currentPage = signal(1);
  pageSize = signal(12);
  totalPages = signal(1);


  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {

    this.productService.getProducts(
      this.currentPage(),
      this.pageSize()
    ).subscribe({

      next: (response: PaginatedResponse<Product>) => {

        this.products.set(response.content);
        this.currentPage.set(response.page);
        this.totalPages.set(response.totalPages);
        this.totalElements = response.totalElements;

      },

      error: (error) => {
        console.error('Error obteniendo productos:', error);
      }

    });
  }


  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;
    this.currentPage.set(page);
    this.loadProducts();
  }

  nextPage(): void { this.goToPage(this.currentPage() + 1); }
  prevPage(): void { this.goToPage(this.currentPage() - 1); }
}