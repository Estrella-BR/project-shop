import { Component, OnInit, inject } from '@angular/core';
import { ApiService } from '../services/api';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <section class="welcome">
      <h1>Angular + Spring Boot</h1>
      <p>{{ mensaje }}</p>
    </section>
  `,
  styles: `
    h1 {
      color: #0f172a;
      font-size: clamp(2rem, 5vw, 3rem);
      margin: 0 0 0.75rem;
    }

    p {
      color: #475569;
      margin: 0;
    }
  `,
})
export class HomeComponent implements OnInit {
  private readonly apiService = inject(ApiService);

  mensaje = '';

  ngOnInit(): void {
    this.apiService.getHello().subscribe({
      next: (respuesta) => {
        this.mensaje = respuesta;
      },
      error: (error) => {
        console.error(error);
      },
    });
  }
}
