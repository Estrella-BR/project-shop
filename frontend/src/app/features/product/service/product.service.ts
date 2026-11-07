import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Product } from '../model/Product';
import { PaginatedResponse } from '../../common/model/PaginatedResponse';

type CatalogProductResponse = Omit<Product, 'quantity'> & { cuantity: string };

@Injectable({
  providedIn: 'root'
})
export class ProductService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}


  getProducts(
    page: number = 0,
    size: number = 20
  ): Observable<PaginatedResponse<Product>> {

    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PaginatedResponse<CatalogProductResponse>>(
      this.apiUrl + '/catalog',
      { params }
    ).pipe(
      map(response => ({
        ...response,
        content: response.content.map(({ cuantity, ...product }) => ({
          ...product,
          quantity: cuantity
        }))
      }))
    );
  }

  getProductWithFilters(
    page: number = 0,
    size: number = 20,
    name?: string,
    category?: string,
    minPrice?: string,
    maxPrice?: string,
    supermarket?: string,
    sortBy?: string 
  ): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('name', name || '')
      .set('category', category || '')
      .set('minPrice', minPrice || '')
      .set('maxPrice', maxPrice || '')
      .set('supermarket', supermarket || '')
      .set('sortByPrice', sortBy || '');

    return this.http.get<PaginatedResponse<CatalogProductResponse>>(
      this.apiUrl + '/catalog/search',
      { params }  
    ).pipe(
      map(response => ({
        ...response,
        content: response.content.map(({ cuantity, ...product }) => ({
          ...product,
          quantity: cuantity
        }))
      }))
    );
  }

}
