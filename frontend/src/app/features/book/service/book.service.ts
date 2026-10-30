import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BookService {
    private apiUrl = 'http://localhost:8080';

     constructor(private http: HttpClient) {}

  getBooks(): Observable<string> {
    return this.http.get(
      `${this.apiUrl}/books`,
      { responseType: 'text' }
    );
  }
}
