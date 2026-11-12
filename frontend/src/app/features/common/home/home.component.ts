
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

  supermercados = [
    { nombre: 'Mercadona', color: '#00a6a6' },
    { nombre: 'Consum', color: '#e53935' },
    { nombre: 'DIA', color: '#e52329' }
  ];

  pasos = [
    {
      numero: '01',
      icono: '🔎',
      titulo: 'Busca productos',
      descripcion: 'Encuentra los productos que necesitas utilizando el buscador y los filtros.'
    },
    {
      numero: '02',
      icono: '⚖️',
      titulo: 'Compara precios',
      descripcion: 'Consulta cuánto cuesta un mismo producto en diferentes supermercados.'
    },
    {
      numero: '03',
      icono: '🛒',
      titulo: 'Elige la mejor opción',
      descripcion: 'Descubre dónde puedes comprar más barato y organiza mejor tu compra.'
    }
  ];
}
