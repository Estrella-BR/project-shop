import { Component, output, signal } from '@angular/core';

export interface SearchFilters {
  minPrice: string;
  maxPrice: string;
  supermarket: string;
  sortBy: string;
}

@Component({
  selector: 'app-search-sign',
  standalone: true,
  templateUrl: './search-sing-component.html',
  styleUrl: './search-sing-component.css',
})
export class SearchSignComponent {

  sellers = signal<string[]>(['Mercadona', 'Consum', 'Dia']);
  searchChange = output<string>();
  filtersChange = output<SearchFilters>();

  filtersOpen = signal(false);

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

  updateFilter(
    filter: keyof SearchFilters,
    event: Event
  ) {
    const input = event.target as HTMLInputElement | HTMLSelectElement;

    this.filters[filter] = input.value as string;

    this.filtersChange.emit({ ...this.filters });
  }

  clearFilters() {
    this.filters = {
      minPrice: '',
      maxPrice: '',
      supermarket: '',
      sortBy: ''
    };

    this.filtersChange.emit({ ...this.filters });
  }
}