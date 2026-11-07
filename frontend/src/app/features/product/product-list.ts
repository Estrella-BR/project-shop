import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from './service/product.service';
import { Product } from './model/Product';
import { PaginatedResponse } from '../common/model/PaginatedResponse';
import { ShelfComponent } from './components/shelf-component/shelf-component';
import { SearchSignComponent } from './components/search-sing-component/search-sing-component';
import { ShelfTopComponent } from './components/search-sing-component/shelf-top-component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, SearchSignComponent, ShelfComponent, ShelfTopComponent],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductListComponent implements OnInit {

  productService = inject(ProductService);
  
  products = signal<Product[]>([]);
  
  isLoading = signal<boolean>(false); 
  
  page = signal(1);
  size = signal(12);
  totalPages = signal(1);
  totalElements = signal(0);
  
  itemPerShelf = signal(4);

  shelves = computed(() => {
    const prods = this.products();
    const result: Product[][] = [];
    for (let i = 0; i < prods.length; i += this.itemPerShelf()) {
      result.push(prods.slice(i, i + this.itemPerShelf()));
    }
    return result;
  });

  ngOnInit(): void {
    this.getFilteredProducts('');
  }

  onSearch(search: string): void {
      this.getFilteredProducts(search);
  }

  loadResponse(response: PaginatedResponse<Product>): void {
    this.products.set(response.content);
    this.page.set(response.page);
    this.totalPages.set(response.totalPages);
    this.totalElements.set(response.totalElements);

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  getAllProducts(): void {
    this.isLoading.set(true);

    this.productService.getProducts(this.page(), this.size()).subscribe({
      next: (response: PaginatedResponse<Product>) => {
        this.loadResponse(response);
      },
      error: (error) => {
        console.error('Error obteniendo productos:', error);
      }
    });
    
    this.isLoading.set(false); 
  }

  getFilteredProducts(search: string): void {
    
    this.isLoading.set(true);

    this.productService.getProductWithFilters(this.page(), this.size(), search).subscribe({
      next: (response: PaginatedResponse<Product>) => {
        this.loadResponse(response)
      },
      error: (error) => {
        console.error('Error obteniendo productos:', error);
      }
    });
    this.isLoading.set(false); 
  }

  nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.update(p => p + 1);
      this.getAllProducts();
    }
  }

  prevPage(): void {
    if (this.page() > 1) {
      this.page.update(p => p - 1);
      this.getAllProducts();
    }
  }

  goToPage(newPage: number): void {
    if (newPage >= 1 && newPage <= this.totalPages() && newPage !== this.page()) {
      this.page.set(newPage);
      this.getAllProducts();
    }
  }
}