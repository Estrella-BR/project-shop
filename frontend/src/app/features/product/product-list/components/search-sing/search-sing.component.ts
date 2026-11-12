import { Component, output, signal, OnInit } from '@angular/core';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Product } from '../../../model/Product';

export interface SearchFilters {
  minPrice: string;
  maxPrice: string;
  supermarket: string;
  sortBy: string;
}

@Component({
  selector: 'app-search-sign',
  standalone: true,
  imports: [
    MatSelectModule,
    MatFormFieldModule
  ],
  templateUrl: './search-sing.component.html',
  styleUrl: './search-sing.component.css',
}) 
export class SearchSignComponent implements OnInit {

    ngOnInit(): void {
      this.selectedSupermarkets.set([this.sellers().join(',')]);
    }

  sellers = signal<string[]>(['Mercadona', 'Consum', 'Dia']);

  searchChange = output<string>();
  filtersChange = output<SearchFilters>();

  filtersOpen = signal(false);

  selectedSupermarkets = signal<string[]>([]);

  filters: SearchFilters = {
    minPrice: '',
    maxPrice: '',
    supermarket: '',
    sortBy: ''
  };

  toggleFilters() {
    this.filtersOpen.update(value => !value);
    this.clearFilters();
  }

  onInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchChange.emit(input.value);
  }

  updateFilter(filter: keyof SearchFilters, event: Event) {
    const input = event.target as HTMLInputElement | HTMLSelectElement;

    this.filters[filter] = input.value;

    this.filtersChange.emit({ ...this.filters });
  }

  updateSupermarkets(selected: string[]) {
    this.selectedSupermarkets.set(selected);

    let supermarketString = selected.join(',');
    supermarketString = supermarketString.toLowerCase();
    
    this.filters.supermarket = supermarketString;

    console.log('Selected supermarkets:', this.filters.supermarket);

    this.filtersChange.emit({ ...this.filters });
  }

  clearFilters() {
    this.selectedSupermarkets.set([]);

    this.filters = {
      minPrice: '',
      maxPrice: '',
      supermarket: '',
      sortBy: ''
    };

    this.filtersChange.emit({ ...this.filters });
  }
}