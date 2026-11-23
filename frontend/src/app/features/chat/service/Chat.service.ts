import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface Ingredient {
  nombre: string;
  cantidad: string;
}

export interface CatalogProduct {
  nombre?: string;
  formato?: string;
  vendedor?: string;
  precio?: string | number;
  imagen?: string;
}

export interface CatalogCheck {
  ingrediente: string;
  cantidad_necesaria: string;
  encontrado: boolean;
  producto: CatalogProduct | null;
}

export interface AteneaAnswer {
  plato: string;
  comensales: number;
  ingredientes_necesarios: Ingredient[];
  comprobacion_catalogo: CatalogCheck[];
}

export interface ChatApiResponse {
  ok: boolean;
  answer: AteneaAnswer | string;
}

@Injectable({
  providedIn: 'root'
})
export class AiChatService {

  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  sendMessage(message: string): Observable<ChatApiResponse> {
    return this.http.post<ChatApiResponse>(
      `${this.apiUrl}/chat`,
      { question: message }
    );
  }
}