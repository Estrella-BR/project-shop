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
  template: `
    <div class="sign-container">

      <div class="ceiling-bracket left"></div>
      <div class="ceiling-bracket right"></div>

      <div class="metal-sign">

        <div class="rivet top-left"></div>
        <div class="rivet top-right"></div>
        <div class="rivet bottom-left"></div>
        <div class="rivet bottom-right"></div>

        <div class="sign-header">
          <h2>SECCIÓN: BÚSQUEDA</h2>
        </div>

        <div class="sign-body">

          <div class="search-input-wrapper">
            <span class="search-icon">🔍</span>

            <input
              type="text"
              placeholder="Escribe el nombre del producto..."
              (input)="onInput($event)"
              class="metal-input"
            >
          </div>

        </div>

        <!-- POST-IT -->
        <div
          class="post-it"
          [class.expanded]="filtersOpen()"
        >

          <button
            class="post-it-header"
            (click)="toggleFilters()"
            [attr.aria-expanded]="filtersOpen()"
          >
            <span>📌 Más filtros</span>

            <span class="arrow">
              {{ filtersOpen() ? '−' : '+' }}
            </span>
          </button>

          @if (filtersOpen()) {
            <div class="post-it-content">
              <div class="filter-field">
                <label>Precio mínimo (€)</label>
                <input
                  type="number"
                  min="0"
                  placeholder="0"
                  (input)="updateFilter('minPrice', $event)"
                >
              </div>

              <div class="filter-field">
                <label>Precio máximo (€)</label>
                <input
                  type="number"
                  min="0"
                  placeholder="Sin límite"
                  (input)="updateFilter('maxPrice', $event)"
                >
              </div>

              <div class="filter-field">
                <label>Supermercado</label>
                <select
                  (change)="updateFilter('supermarket', $event)"
                >
                  <option value="">Todos</option>
                  <option value="Mercadona">Mercadona</option>
                  <option value="Carrefour">Carrefour</option>
                </select>
              </div>

              <div class="filter-field">
                <label>Ordenar por</label>
                <select
                  (change)="updateFilter('sortBy', $event)"
                >
                  <option value="">Sin orden</option>
                  <option value="asc">Precio: menor a mayor</option>
                  <option value="desc">Precio: mayor a menor</option>
                </select>
              </div>
            </div>
          }

        </div>

      </div>
    </div>
  `,

  styles: [`
    .sign-container {
      display: flex;
      justify-content: center;
      padding-top: 30px;
      margin-bottom: 30px;
      margin-top: 10px;
      position: relative;
    }

    .ceiling-bracket {
      position: absolute;
      top: -10px;
      width: 10px;
      height: 40px;
      background: linear-gradient(to right, #888, #ccc, #888);
      border: 1px solid #444;
      border-radius: 2px;
      z-index: 1;
    }

    .ceiling-bracket.left { left: 35%; }
    .ceiling-bracket.right { right: 35%; }

    .metal-sign {
      background: linear-gradient(135deg, #e0e0e0 0%, #f8f8f8 40%, #b0b0b0 100%);
      border: 3px solid #666;
      border-radius: 8px;
      box-shadow:
        0 10px 25px rgba(0,0,0,0.6),
        inset 0 2px 3px rgba(255,255,255,0.8),
        inset 0 -2px 5px rgba(0,0,0,0.3);
      position: relative;
      width: 500px;
      display: flex;
      flex-direction: column;
      overflow: visible;
    }

    .rivet {
      position: absolute;
      width: 12px;
      height: 12px;
      background: radial-gradient(circle at 30% 30%, #fff, #999, #444);
      border-radius: 50%;
      box-shadow: 1px 1px 2px rgba(0,0,0,0.5);
      z-index: 10;
    }

    .top-left { top: 8px; left: 8px; }
    .top-right { top: 8px; right: 8px; }
    .bottom-left { bottom: 8px; left: 8px; }
    .bottom-right { bottom: 8px; right: 8px; }

    .sign-header {
      background: #d92b2b;
      padding: 12px 0;
      text-align: center;
      border-bottom: 3px solid #a31d1d;
      box-shadow: inset 0 -5px 10px rgba(0,0,0,0.2);
      border-radius: 4px 4px 0 0;
    }

    .sign-header h2 {
      margin: 0;
      color: #fff;
      font-family: 'Arial Black', Impact, sans-serif;
      font-size: 1.3rem;
      letter-spacing: 2px;
      text-shadow: 2px 2px 0px rgba(0,0,0,0.4);
    }

    .sign-body {
      padding: 25px 30px;
      display: flex;
      justify-content: center;
    }

    .search-input-wrapper {
      position: relative;
      width: 100%;
      display: flex;
      align-items: center;
    }

    .search-icon {
      position: absolute;
      left: 15px;
      font-size: 1.2rem;
      z-index: 2;
    }

    .metal-input {
      width: 100%;
      padding: 12px 15px 12px 45px;
      border: 2px solid #888;
      border-radius: 4px;
      background: #f0f0f0;
      font-size: 1rem;
      color: #222;
      outline: none;
      box-sizing: border-box;
      box-shadow: inset 0 3px 6px rgba(0,0,0,0.2);
      transition: all 0.2s ease;
    }

    .metal-input:focus {
      border-color: #d92b2b;
      background: #fff;
      box-shadow: 0 0 8px rgba(217,43,43,0.4);
    }

    .metal-input::placeholder {
      color: #888;
      font-weight: bold;
    }

    /* POST-IT */

    .post-it {
      position: absolute;
      top: -12px;
      /* Anchored on the LEFT so growing width pushes it to the RIGHT */
      left: calc(100% - 103px); /* right edge overhangs the sign by 22px */
      width: 125px;
      background: #fff19a;
      color: #4b4328;
      z-index: 20;
      transform: rotate(5deg);
      box-shadow: 3px 5px 9px rgba(0,0,0,0.3);
      transition:
        width 0.3s ease,
        transform 0.3s ease;
      border-bottom: 3px solid #e5d36d;
      border-radius: 2px;
    }

    .post-it::after {
      content: '';
      position: absolute;
      bottom: -1px;
      right: 0;
      width: 18px;
      height: 18px;
      background: linear-gradient(
        135deg,
        #e5d36d 50%,
        #fff19a 50%
      );
      clip-path: polygon(100% 0, 100% 100%, 0 100%);
    }

    .post-it:hover {
      transform: rotate(2deg) translateY(-3px);
    }

    /* Expands horizontally to the right, two filters per column */
    .post-it.expanded {
      width: 430px;
      transform: rotate(0deg);
    }

    .post-it-header {
      width: 100%;
      border: none;
      background: transparent;
      padding: 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      color: #4b4328;
      font-weight: bold;
      font-size: 0.9rem;
      font-family: 'Comic Sans MS', cursive;
    }

    .arrow {
      font-size: 1.3rem;
    }

    /* Two filters per row */
    .post-it-content {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 10px 14px;
      padding: 5px 14px 18px;
      font-family: Arial, sans-serif;
      animation: appear 0.25s ease;
    }

    .post-it-content h3 {
      grid-column: 1 / -1;
      margin: 0 0 2px;
      font-family: 'Comic Sans MS', cursive;
      font-size: 1rem;
    }

    .filter-field {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .post-it-content label {
      font-size: 0.78rem;
      font-weight: bold;
    }

    .post-it-content input,
    .post-it-content select {
      width: 100%;
      box-sizing: border-box;
      padding: 7px;
      border: 1px solid #c5b45b;
      border-radius: 3px;
      background: #fffbea;
      color: #333;
      font-size: 0.8rem;
      outline: none;
    }

    .post-it-content input:focus,
    .post-it-content select:focus {
      border-color: #a18b24;
    }

    .clear-button {
      grid-column: 1 / -1;
      margin-top: 6px;
      padding: 8px;
      background: #e7d36a;
      border: 1px solid #bca942;
      border-radius: 3px;
      cursor: pointer;
      font-weight: bold;
      color: #4b4328;
    }

    .clear-button:hover {
      background: #dbc34b;
    }

    @keyframes appear {
      from {
        opacity: 0;
        transform: translateY(-5px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /*
      Fallback: if the viewport isn't wide enough to open to the right
      without clipping, anchor to the right edge again (opens left).
    */
    @media (max-width: 1170px) {
      .post-it {
        left: auto;
        right: -22px;
      }

      .post-it.expanded {
        width: min(400px, calc(100vw - 40px));
      }
    }

    @media (max-width: 600px) {
      .metal-sign {
        width: calc(100% - 40px);
      }

      .post-it {
        left: auto;
        right: -10px;
      }

      .post-it.expanded {
        width: min(340px, calc(100vw - 50px));
        right: -5px;
      }

      /* Single column so the selects don't get squeezed */
      .post-it-content {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class SearchSignComponent {

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