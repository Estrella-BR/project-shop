import { Component, OnInit, inject, signal } from '@angular/core';

import { ProductService } from './service/product.service';
import { Product } from './model/Product';

@Component({
    selector: 'app-product-list',
    standalone: true,
    imports: [],
    templateUrl: './product-list.html',
    styleUrl: './product-list.css',
})
export class ProductListComponent implements OnInit {
    productService  = inject(ProductService);
    products = signal<Product[]>([]);

    ngOnInit(): void {
        this.productService.getProducts().subscribe({
            next: (data) => {
                this.products.set(JSON.parse(data) as Product[]);
                
            },
            error: (error) => {
                console.error('Error fetching products:', error);
            }
        });
    }

}