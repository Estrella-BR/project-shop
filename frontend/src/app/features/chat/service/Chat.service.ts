import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CatalogCheck, ChatApiResponse } from '../model/ChatMessage';
import { Product } from '../../common/model/Product';


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


  mapCatalogProduct(product: NonNullable<CatalogCheck['producto']>): Product {
    const result = new Product();

    result.name = product.nombre ?? '';
    result.price = String(product.precio ?? '');
    result.quantity = product.formato ?? '';
    result.image = product.imagen ?? '';
    result.cleanImage = '';
    result.url = product.url ?? '';
    result.seller = product.vendedor ?? '';

    return result;
  }
}