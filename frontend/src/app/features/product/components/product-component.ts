import { Component, Input, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from './../model/Product';
@Component({
  selector: 'app-product-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-component.html',
  styleUrl: './product-component.css',
  host: {
  },
})
export class ProductComponent implements OnInit {
    @Input() product: Product | null = null;

    ngOnInit(): void {
    }

}

