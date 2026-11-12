import { Component, input, model, effect } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css'],
})
export class PaginationComponent {
  page = model<number>(1);

  /** Total de páginas disponibles */
  totalPages = input.required<number>();

  constructor() {
    // Si cambia totalPages y la página actual queda fuera de rango, la corregimos
    effect(() => {
      const total = this.totalPages();
      if (this.page() > total) {
        this.page.set(total);
      } else if (this.page() < 1) {
        this.page.set(1);
      }
    });
  }

  prevPage(): void {
    if (this.page() > 1) {
      this.page.set(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.set(this.page() + 1);
    }
  }
}