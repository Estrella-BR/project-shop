import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiService } from './services/api';

@Component({
  selector: 'app-root',
  template: `
    <h1>Angular + Spring Boot</h1>

    <p>{{ mensaje }}</p>
  `
})
export class App implements OnInit {

  mensaje = '';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.getHello().subscribe({
      next: respuesta => {
        this.mensaje = respuesta;
      },
      error: error => {
        console.error(error);
      }
    });
  }
}