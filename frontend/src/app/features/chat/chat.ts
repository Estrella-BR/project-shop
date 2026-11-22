import { Component, inject, signal } from '@angular/core';
import { AiChatService } from './service/Chat.service';
import { ChatMessage } from './model/ChatMessage';
import { ChatInputComponent } from './components/chat-input/chat-input';
import { ChatMessageComponent } from './components/chat-message/chat-message';

@Component({
  imports: [
    ChatInputComponent,
    ChatMessageComponent
  ],
  selector: 'app-chat',
  styleUrl: './chat.css',
  templateUrl: './chat.html',
})
export class ChatComponent {

  private aiService = inject(AiChatService);

  messages = signal<ChatMessage[]>([
    {
      id: 1,
      role: 'assistant',
      content: '¡Hola! 👋 Soy tu asistente de compra. ¿Qué estás buscando hoy?',
      timestamp: new Date()
    }
  ]);

  loading = signal(false);

  sendMessage(content: string): void {
  if (!content.trim() || this.loading()) return;

  const userMessage: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content,
    timestamp: new Date()
  };

  const loadingMessage: ChatMessage = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    loading: true
  };

  this.messages.update(m => [...m, userMessage, loadingMessage]);
  this.loading.set(true);

  this.aiService.sendMessage(content).subscribe({
    next: (response) => {
      this.messages.update(m => [
        ...m.slice(0, -1),
        { ...loadingMessage, content: response, loading: false, timestamp: new Date() }
      ]);
      this.loading.set(false);
    },
    error: () => {
      this.messages.update(m => [
        ...m.slice(0, -1),
        {
          ...loadingMessage,
          content: 'Lo siento, ha ocurrido un error. Inténtalo de nuevo.',
          loading: false
        }
      ]);
      this.loading.set(false);
    }
  });
}
}
