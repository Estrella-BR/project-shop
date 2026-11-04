import { Component } from '@angular/core';

@Component({
  selector: 'app-shelf-top',
  standalone: true,
  template: `
    <div class="shelf-top-container">
      
      <!-- Estructura superior -->
      <div class="top-frame">
        <!-- Detalles decorativos del marco -->
        <div class="frame-panel left"></div>
        <div class="frame-panel center"></div>
        <div class="frame-panel right"></div>
      </div>

      <!-- Soportes verticales que conectan con el primer estante -->
      <div class="brackets">
        <div class="bracket"></div>
        <div class="bracket"></div>
        <div class="bracket"></div>
      </div>
      
    </div>
  `,
  styles: [`
    .shelf-top-container {
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: -4px; /* Se superpone ligeramente al primer estante */
      z-index: 5;
      position: relative;
    }

    /* --- MARCO SUPERIOR (Techo de la estantería) --- */
    .top-frame {
      width: 100%;
      height: 40px;
      background-color: #6b7280; /* Gris oscuro del fondo */
      border: 4px solid #1a1a24; /* Borde negro cartoon */
      border-bottom: none;
      border-radius: 12px 12px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      padding: 0 20px;
      box-sizing: border-box;
      box-shadow: 4px -4px 0px rgba(0,0,0,0.3); /* Sombra hacia arriba */
      position: relative;
    }

    /* Paneles decorativos en el marco */
    .frame-panel {
      height: 20px;
      background-color: #9ca3af; /* Gris claro */
      border: 3px solid #1a1a24;
      border-bottom: none;
      border-radius: 6px 6px 0 0;
      box-shadow: inset 0 4px 0 rgba(255,255,255,0.2); /* Brillo superior */
    }

    .frame-panel.left { width: 25%; }
    .frame-panel.center { width: 40%; height: 28px; } /* El del centro es más alto */
    .frame-panel.right { width: 25%; }

    /* --- SOPORTES VERTICALES --- */
  `]
})
export class ShelfTopComponent {}