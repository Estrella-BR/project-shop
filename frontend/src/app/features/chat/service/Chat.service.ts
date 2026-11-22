import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AiChatService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/ai/chat';

  sendMessage(message: string): Observable<{ response: string }> {
    return this.http.post<{ response: string }>(
      this.apiUrl,
      { message }
    );
  }
}