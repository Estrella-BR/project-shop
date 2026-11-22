import { Component, output, input, signal } from '@angular/core';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  templateUrl: './chat-input.html',
  styleUrl: './chat-input.css'
})
export class ChatInputComponent {

  disabled = input(false);
  send = output<string>();

  message = signal('');

  submit(): void {
    const value = this.message().trim();

    if (!value || this.disabled()) return;

    this.send.emit(value);
    this.message.set('');
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submit();
    }
  }
}