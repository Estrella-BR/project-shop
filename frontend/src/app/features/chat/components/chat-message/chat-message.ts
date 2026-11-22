import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ChatMessage } from '../../model/ChatMessage';

@Component({
  imports: [],
  selector: 'app-chat-message',
  styleUrl: './chat-message.css',
  templateUrl: './chat-message.html',
})
export class ChatMessageComponent {
    message = input.required<ChatMessage>();

}
