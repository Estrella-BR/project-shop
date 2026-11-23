import { Component, inject, signal } from '@angular/core';
import { AteneaAnswer, ChatApiResponse } from './model/ChatMessage';
import { AiChatService } from './service/Chat.service';
import { ChatMessage } from './model/ChatMessage';
import { ChatInputComponent } from './components/chat-input/chat-input';
import { ChatMessageComponent } from './components/chat-message/chat-message';
import { Product } from '../common/model/Product';

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
      content: '¡Hola! Soy Atenea, tu asistente de compra. ¿Qué te gustaría preparar hoy?',
      timestamp: new Date()
    }
  ]);

  loading = signal(false);

  sendMessage(content: string): void {
    if (!content.trim() || this.loading()) return;

    const userMessage: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: content.trim(),
      timestamp: new Date()
    };

    const loadingMessage: ChatMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      loading: true
    };

    this.messages.update(messages => [
      ...messages,
      userMessage,
      loadingMessage
    ]);

    this.loading.set(true);

    this.aiService.sendMessage(content.trim()).subscribe({
      next: response => {
        const result = this.formatResponse(response);

        this.replaceLoadingMessage(loadingMessage.id, {
          content: result.content,
          products: result.products,
          loading: false,
          timestamp: new Date()
        });

        this.loading.set(false);
      },
      error: error => {
        console.error('Error communicating with Atenea:', error);

        this.replaceLoadingMessage(loadingMessage.id, {
          content: 'Lo siento, ha ocurrido un error al comunicarme con Atenea. Inténtalo de nuevo.',
          loading: false,
          timestamp: new Date()
        });

        this.loading.set(false);
      }
    });
  }

  private replaceLoadingMessage(
    id: number,
    updates: Partial<ChatMessage>
  ): void {
    this.messages.update(messages =>
      messages.map(message =>
        message.id === id
          ? { ...message, ...updates }
          : message
      )
    );
  }

  private formatResponse(response: ChatApiResponse): {
    content: string;
    products: Product[];
  } {
    if (!response.ok) {
      return {
        content: 'No he podido procesar tu consulta.',
        products: []
      };
    }

    if (typeof response.answer === 'string') {
      return {
        content: response.answer,
        products: []
      };
    }

    return this.formatCookingResponse(response.answer);
  }

  private formatCookingResponse(answer: AteneaAnswer): {
  content: string;
  products: Product[];
} {
  let content = `🍽️ ${answer.plato} para ${answer.comensales} personas\n\n`;

  content += '🥕 INGREDIENTES NECESARIOS:\n';

  for (const ingredient of answer.ingredientes_necesarios ?? []) {
    content += `• ${ingredient.nombre}: ${ingredient.cantidad}\n`;
  }

  const products: Product[] = [];

  for (const item of answer.comprobacion_catalogo ?? []) {
    if (item.encontrado && item.producto) {
      products.push(
        this.aiService.mapCatalogProduct(item.producto)
      );
    }
  }

  return { content, products };
}
}