import { Component, inject, signal } from '@angular/core';
import { AiChatService, AteneaAnswer, ChatApiResponse } from './service/Chat.service';
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
      content: '¡Hola! 👋 Soy Atenea, tu asistente de compra. ¿Qué te gustaría preparar hoy?',
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
      next: (response) => {
        const answer = this.formatResponse(response);

        this.replaceLoadingMessage(loadingMessage.id, answer);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error communicating with Atenea:', error);

        this.replaceLoadingMessage(
          loadingMessage.id,
          'Lo siento, ha ocurrido un error al comunicarme con Atenea. Inténtalo de nuevo.'
        );

        this.loading.set(false);
      }
    });
  }

  private replaceLoadingMessage(id: number, content: string): void {
    this.messages.update(messages =>
      messages.map(message =>
        message.id === id
          ? {
              ...message,
              content,
              loading: false,
              timestamp: new Date()
            }
          : message
      )
    );
  }

  private formatResponse(response: ChatApiResponse): string {
    if (!response.ok) {
      return 'No he podido procesar tu consulta.';
    }

    // Atenea puede devolver un texto si la consulta no es de cocina.
    if (typeof response.answer === 'string') {
      return response.answer;
    }

    return this.formatCookingResponse(response.answer);
  }

  private formatCookingResponse(answer: AteneaAnswer): string {
    let result = `🍽️ **${answer.plato}** para ${answer.comensales} personas\n\n`;

    result += '**Ingredientes necesarios:**\n';

    for (const ingredient of answer.ingredientes_necesarios ?? []) {
      result += `• ${ingredient.nombre}: ${ingredient.cantidad}\n`;
    }

    result += '\n**Productos encontrados en el catálogo:**\n';

    for (const item of answer.comprobacion_catalogo ?? []) {
      if (item.encontrado && item.producto) {
        const product = item.producto;

        result += `\n✅ **${item.ingrediente}** (${item.cantidad_necesaria})\n`;
        result += `   ${product.nombre ?? 'Producto encontrado'}\n`;

        if (product.formato) {
          result += `   Formato: ${product.formato}\n`;
        }

        if (product.vendedor) {
          result += `   Supermercado: ${product.vendedor}\n`;
        }

        if (product.precio !== undefined) {
          result += `   Precio: ${product.precio} €\n`;
        }
      } else {
        result += `\n❌ ${item.ingrediente}: no encontrado en el catálogo.\n`;
      }
    }

    return result;
  }
}