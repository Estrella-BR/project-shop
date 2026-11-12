import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from './service/product.service';
import { Product } from './model/Product';
import { PaginatedResponse } from '../common/model/PaginatedResponse';
import { ShelfComponent } from './components/shelf-component/shelf-component';
import { SearchSignComponent } from './components/search-sing-component/search-sing-component';
import { SpinnerComponent } from './components/spinner-component/spinner.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, SearchSignComponent, ShelfComponent, SpinnerComponent],
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

  nameFilter = signal<string>('');
  categoryFilter = signal<string>('');
  minPriceFilter = signal<string>('');
  maxPriceFilter = signal<string>('')
  supermarketFilter = signal<string>('');
  sortByFilter = signal<string>('');

  shelves = computed(() => {
    const prods = this.products();
    const result: Product[][] = [];
    for (let i = 0; i < prods.length; i += this.itemPerShelf()) {
      result.push(prods.slice(i, i + this.itemPerShelf()));
    }
    return result;
  });

  ngOnInit(): void {
    this.getFilteredProducts();
  }

  onSearch(search: string): void {
    this.nameFilter.set(search);
    this.page.set(1); 
    this.getFilteredProducts();
  }

  onFilterChange(filters: {category?: string, minPrice?: string; maxPrice?: string; supermarket?: string, sortBy?: string}): void {
    this.page.set(1);
    this.categoryFilter.set(filters.category || '');
    this.minPriceFilter.set(filters.minPrice || '');
    this.maxPriceFilter.set(filters.maxPrice || '');
    this.supermarketFilter.set(filters.supermarket || '');
    this.sortByFilter.set(filters.sortBy || '');
    this.getFilteredProducts();
  }

  loadResponse(response: PaginatedResponse<Product>): void {
    this.products.set(response.content);
    this.page.set(response.page );
    this.totalPages.set(response.totalPages-1);
    this.totalElements.set(response.totalElements);

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  
  getFilteredProducts( ): void {
    
    this.isLoading.set(true);

    this.productService.getProductWithFilters(this.page(), this.size(), this.nameFilter(), this.categoryFilter(), this.minPriceFilter(), this.maxPriceFilter(), this.supermarketFilter(), this.sortByFilter()).subscribe({
      next: (response: PaginatedResponse<Product>) => {
        this.loadResponse(response)
        this.isLoading.set(false); 
      },
      error: (error) => {
        console.error('Error obteniendo productos:', error);
        
        this.isLoading.set(false); 
      }
    });
  }

  nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.update(p => p + 1);
      this.getFilteredProducts();
    }
  }

  prevPage(): void {
    if (this.page() > 1) {
      this.page.update(p => p - 1);
      this.getFilteredProducts();
    }
  }

}