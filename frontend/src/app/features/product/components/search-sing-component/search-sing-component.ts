import { Component, output } from '@angular/core';

@Component({
  selector: 'app-search-sign',
  standalone: true,
  template: `
    <div class="sign-container">
      <!-- Soportes de techo simulados -->
      <div class="ceiling-bracket left"></div>
      <div class="ceiling-bracket right"></div>

      <div class="metal-sign">
        <!-- Tornillos de las esquinas -->
        <div class="rivet top-left"></div>
        <div class="rivet top-right"></div>
        <div class="rivet bottom-left"></div>
        <div class="rivet bottom-right"></div>

        <!-- Encabezado estilo pasillo de supermercado -->
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
      </div>
    </div>
  `,
  styles: [`
    /* --- CONTENEDOR PRINCIPAL --- */
    .sign-container {
      display: flex;
      justify-content: center;
      padding-top: 30px;
      margin-bottom: 30px;
      margin-top: 10px;
      position: relative;
    }

    /* --- SOPORTES DE TECHO (Simulan cómo se cuelga) --- */
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

    /* --- PLACA DE METAL PRINCIPAL --- */
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
      overflow: hidden;
    }

    /* --- TORNILLOS / REMACHES --- */
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

    /* --- ENCABEZADO ROJO (Estilo señalética) --- */
    .sign-header {
      background: #d92b2b; /* Rojo supermercado */
      padding: 12px 0;
      text-align: center;
      border-bottom: 3px solid #a31d1d;
      box-shadow: inset 0 -5px 10px rgba(0,0,0,0.2);
    }

    .sign-header h2 {
      margin: 0;
      color: #fff;
      font-family: 'Arial Black', Impact, sans-serif;
      font-size: 1.3rem;
      letter-spacing: 2px;
      text-transform: uppercase;
      text-shadow: 2px 2px 0px rgba(0,0,0,0.4);
    }

    /* --- CUERPO DEL CARTEL --- */
    .sign-body {
      padding: 25px 30px;
      display: flex;
      justify-content: center;
    }

    /* --- INPUT ESTILO INDUSTRIAL --- */
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
      color: #666;
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
      /* Sombra interior para que parezca un hueco en el metal */
      box-shadow: inset 0 3px 6px rgba(0,0,0,0.2), 0 1px 0 rgba(255,255,255,0.8);
      transition: all 0.2s ease;
      font-family: Arial, sans-serif;
    }

    .metal-input:focus {
      border-color: #d92b2b; /* Brillo rojo al enfocar */
      background: #fff;
      box-shadow: inset 0 3px 6px rgba(0,0,0,0.1), 0 0 8px rgba(217, 43, 43, 0.4);
    }

    .metal-input::placeholder {
      color: #888;
      font-weight: bold;
    }
  `]
})
export class SearchSignComponent {
  searchChange = output<string>();

  onInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchChange.emit(input.value);
  }
}