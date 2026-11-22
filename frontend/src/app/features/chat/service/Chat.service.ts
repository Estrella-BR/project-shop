import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AiChatService {

  private http = inject(HttpClient);

  private apiUrl = environment.apiUrl;

  sendMessage(message: string): Observable<string> {
    return this.http.get(this.apiUrl + "/chat", {
      params: { question: message },
      responseType: 'text' as 'json'   // backend returns a plain String, not JSON
    }) as Observable<string>;
  }
}